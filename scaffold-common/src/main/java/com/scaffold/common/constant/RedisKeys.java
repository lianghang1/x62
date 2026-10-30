package com.scaffold.common.constant;

/**
 * 全局 Redis key 前缀契约——所有框架组件的 Redis key 必须引用这里的常量，
 * 禁止硬编码字符串（防拼写漂移，Grafana/运维排查按同一口径检索）。
 *
 * <p>规约：{@code scaffold:{组件}:{用途}:{业务键}}，组件维度一段、用途一段，
 * 业务键拼接在常量之后。</p>
 *
 * @author lianghang
 */
public final class RedisKeys {

    /** 防刷闸门（AbuseGuard：间隔/窗口/天闸计数） */
    public static final String ABUSE = "scaffold:abuse:";

    /** @RateLimit 注解限流计数 */
    public static final String RATE_LIMIT = "scaffold:ratelimit:";

    /** @Idempotent 幂等拦截 */
    public static final String IDEMPOTENT = "scaffold:idempotent:";

    /** 分布式锁（RedisLockUtil） */
    public static final String LOCK = "scaffold:lock:";

    /** 登录会话 token：{token} → 会话 JSON */
    public static final String AUTH_TOKEN = "scaffold:auth:token:";

    /** 账户 → 持有 token 集合索引（全端下线 / 在线用户查询） */
    public static final String AUTH_ACCOUNT = "scaffold:auth:account:";

    /** 计算型验证码：{captchaId} → 算式 */
    public static final String SECURITY_CAPTCHA = "scaffold:security:captcha:";

    /** 登录失败计数（账号维度） */
    public static final String SECURITY_LOGIN_FAIL = "scaffold:security:login:fail:";

    /** 登录失败计数（IP 维度滑动窗口） */
    public static final String SECURITY_LOGIN_IP = "scaffold:security:login:ip:";

    /** OpenAPI 签名 nonce 查重（防重放） */
    public static final String OPENAPI_NONCE = "scaffold:openapi:nonce:";

    /** 通用缓存（starter-cache） */
    public static final String CACHE = "scaffold:cache:";

    /** SSE 多实例推送转发（pub/sub 频道名） */
    public static final String PUSH_RELAY = "scaffold:push:relay";

    /** DB 配置缓存失效广播（pub/sub 频道名，多实例刷新） */
    public static final String CONFIG_REFRESH = "scaffold:config:refresh";

    private RedisKeys() {
    }
}
