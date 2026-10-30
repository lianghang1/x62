package com.scaffold.starter.security.policy;

import com.scaffold.starter.security.config.SecurityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 基于 Redis 计数的登录失败锁定组件（RuoYi maxRetryCount 思路）。
 *
 * <p>两个维度互补：账号维度——同账号连续失败达到 maxFailCount 后锁定
 * lockSeconds，成功登录 reset 清零（保护账号不误伤共享出口 IP）；
 * IP 维度——滑动窗口内同 IP 失败达到 failLimit 后拒绝该 IP 登录
 * （防单 IP 遍历撞库），窗口到期自动解除。IP 计数只在 {@code ipGuard.enabled}
 * 时生效。</p>
 *
 * <p>可观测性：每次失败上报 {@code scaffold.security.login.failures} 计数
 * （dimension=account/ip），registry 缺席时静默。</p>
 *
 * @author lianghang
 */
public class LoginFailureGuard {

    private static final String KEY_PREFIX = "scaffold:security:login:fail:";
    private static final String IP_KEY_PREFIX = "scaffold:security:login:ip:";
    private static final String FAIL_METRIC = com.scaffold.common.constant.MetricsNames.LOGIN_FAILURES;

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties.PasswordPolicy policy;
    private final SecurityProperties.IpGuard ipGuard;
    /** 可选指标注册表：null 表示本环境未启用可观测性 */
    private final MeterRegistry meterRegistry;

    public LoginFailureGuard(StringRedisTemplate redisTemplate,
                             SecurityProperties.PasswordPolicy policy) {
        this(redisTemplate, policy, null, null);
    }

    public LoginFailureGuard(StringRedisTemplate redisTemplate,
                             SecurityProperties.PasswordPolicy policy,
                             SecurityProperties.IpGuard ipGuard) {
        this(redisTemplate, policy, ipGuard, null);
    }

    public LoginFailureGuard(StringRedisTemplate redisTemplate,
                             SecurityProperties.PasswordPolicy policy,
                             SecurityProperties.IpGuard ipGuard,
                             MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.policy = policy;
        this.ipGuard = ipGuard != null ? ipGuard : new SecurityProperties.IpGuard();
        this.meterRegistry = meterRegistry;
    }

    /** 账号是否已被临时锁定 */
    public boolean isLocked(String accountNo) {
        String count = redisTemplate.opsForValue().get(key(accountNo));
        return count != null && Integer.parseInt(count) >= policy.getMaxFailCount();
    }

    /** 账号当前连续失败次数（验证码 after_fail 模式据此决定是否强制验证码） */
    public int currentCount(String accountNo) {
        String count = redisTemplate.opsForValue().get(key(accountNo));
        return count == null ? 0 : Integer.parseInt(count);
    }

    /** 记录一次账号失败，返回当前连续失败次数 */
    public int recordFailure(String accountNo) {
        Long count = redisTemplate.opsForValue().increment(key(accountNo));
        if (count != null && count == 1L) {
            redisTemplate.expire(key(accountNo), Duration.ofSeconds(policy.getLockSeconds()));
        }
        recordMetric("account");
        return count == null ? 0 : count.intValue();
    }

    /** 登录成功清零 */
    public void reset(String accountNo) {
        redisTemplate.delete(key(accountNo));
    }

    /** 该 IP 是否已触发撞库防护（窗口内失败达上限） */
    public boolean isIpBlocked(String ip) {
        if (ip == null || !ipGuard.isEnabled()) {
            return false;
        }
        String count = redisTemplate.opsForValue().get(ipKey(ip));
        return count != null && Long.parseLong(count) >= ipGuard.getFailLimit();
    }

    /** 记录一次该 IP 的登录失败，返回窗口内累计次数 */
    public long recordIpFailure(String ip) {
        if (ip == null || !ipGuard.isEnabled()) {
            return 0;
        }
        Long count = redisTemplate.opsForValue().increment(ipKey(ip));
        if (count != null && count == 1L) {
            redisTemplate.expire(ipKey(ip), Duration.ofSeconds(ipGuard.getWindowSeconds()));
        }
        recordMetric("ip");
        return count == null ? 0 : count;
    }

    /** 登录失败指标上报：registry 缺席时静默。 */
    private void recordMetric(String dimension) {
        if (meterRegistry != null) {
            meterRegistry.counter(FAIL_METRIC, "dimension", dimension).increment();
        }
    }

    private String key(String accountNo) {
        return KEY_PREFIX + accountNo;
    }

    private String ipKey(String ip) {
        return IP_KEY_PREFIX + ip;
    }
}
