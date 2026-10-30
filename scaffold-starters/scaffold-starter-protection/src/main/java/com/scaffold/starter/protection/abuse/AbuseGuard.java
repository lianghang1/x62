package com.scaffold.starter.protection.abuse;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 编程式防刷闸门：短信爆破、撞库、接口滥刷等滥用行为的统一计数入口。
 *
 * <p>与 {@code @RateLimit} 注解（拦 HTTP 入口）互补——注解管「请求维度」，
 * 本组件管「业务动作维度」（发短信、登录失败、领券等代码内动作）。
 * 三种闸门：间隔闸（两次动作最小间隔）、窗口闸（时间窗内累计上限）、
 * 天闸（自然日额度，跨零点自动重置）；全部基于 Redis 固定窗口，
 * 超限抛 {@link AbuseException}（HTTP 429）。</p>
 *
 * <p>【规约】防刷在业务动作「执行前」调用（先判后做）；失败动作（如登录失败）
 * 用 {@link #hit} 累计、成功用 {@link #reset} 清零，语义与登录锁定一致。</p>
 *
 * <p>可观测性：每次拦截上报 {@code scaffold.protection.abuse.rejected} 计数
 * （scene/rule 标签），registry 缺席（lite/无 observability）时静默。</p>
 *
 * @author lianghang
 */
public class AbuseGuard {

    private static final String KEY_PREFIX = "scaffold:abuse:";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REJECT_METRIC = com.scaffold.common.constant.MetricsNames.ABUSE_REJECTED;

    private final StringRedisTemplate redisTemplate;
    /** 可选指标注册表：null 表示本环境未启用可观测性 */
    private final MeterRegistry meterRegistry;

    public AbuseGuard(StringRedisTemplate redisTemplate) {
        this(redisTemplate, null);
    }

    public AbuseGuard(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 间隔闸：同一主体两次动作之间必须至少间隔 {@code intervalSeconds}。
     *
     * <p>典型用途：短信验证码发送间隔（同手机号 60s 一条）。</p>
     *
     * @param scene           防刷场景（如 sms），作为 key 的一部分隔离
     * @param identity        防护主体（手机号/IP/账号等）
     * @param intervalSeconds 最小间隔（秒）
     */
    public void checkInterval(String scene, String identity, int intervalSeconds) {
        String key = key(scene, "interval", identity);
        Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(intervalSeconds));
        if (first == null || !first) {
            recordReject(scene, "interval");
            throw new AbuseException(scene, identity, "操作过于频繁，请 " + intervalSeconds + " 秒后再试");
        }
    }

    /**
     * 窗口闸：同一主体在 {@code window} 时间窗内累计动作不得超过 {@code limit}。
     *
     * <p>调用即计数（先加后判），适合「执行即计」的动作；失败型累计请用 {@link #hit}。</p>
     *
     * @param scene    防刷场景
     * @param identity 防护主体
     * @param limit    窗口内允许的最大次数
     * @param window   计数窗口
     */
    public void checkWindow(String scene, String identity, int limit, Duration window) {
        long current = count(key(scene, "window", identity), window);
        if (current > limit) {
            recordReject(scene, "window");
            throw new AbuseException(scene, identity, "操作过于频繁，请稍后再试");
        }
    }

    /**
     * 天闸：同一主体当日（自然日，零点重置）动作不得超过 {@code dailyLimit}。
     *
     * <p>典型用途：同手机号每日短信上限、同 IP 每日发送上限、全局发送配额。</p>
     *
     * @param scene      防刷场景
     * @param identity   防护主体
     * @param dailyLimit 当日允许的最大次数
     */
    public void checkDaily(String scene, String identity, int dailyLimit) {
        String key = KEY_PREFIX + scene + ":day:" + LocalDate.now().format(DAY) + ":" + identity;
        // TTL 到次日零点 + 余量，跨天后自然失效重建
        long current = count(key, Duration.ofSeconds(secondsUntilMidnight() + 60));
        if (current > dailyLimit) {
            recordReject(scene, "daily");
            throw new AbuseException(scene, identity, "今日操作次数已达上限，请明日再试");
        }
    }

    /**
     * 记一次失败（仅计数不判定），供「失败累计、达到阈值再拦截」的场景。
     *
     * @param scene    防刷场景
     * @param identity 防护主体
     * @param window   计数窗口
     * @return 窗口内累计次数
     */
    public long hit(String scene, String identity, Duration window) {
        return count(key(scene, "window", identity), window);
    }

    /** 查询主体在窗口内的当前计数（不计数）。 */
    public long current(String scene, String identity) {
        String value = redisTemplate.opsForValue().get(key(scene, "window", identity));
        return value == null ? 0 : Long.parseLong(value);
    }

    /** 成功后清零该主体的计数。 */
    public void reset(String scene, String identity) {
        redisTemplate.delete(key(scene, "window", identity));
    }

    /** 计数 +1；首次计数时设置 TTL，返回累计值。 */
    private long count(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return count == null ? 0 : count;
    }

    /** 拦截指标上报：registry 缺席时静默。 */
    private void recordReject(String scene, String rule) {
        if (meterRegistry != null) {
            meterRegistry.counter(REJECT_METRIC, "scene", scene, "rule", rule).increment();
        }
    }

    private String key(String scene, String type, String identity) {
        return KEY_PREFIX + scene + ":" + type + ":" + identity;
    }

    private long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).getSeconds();
    }
}
