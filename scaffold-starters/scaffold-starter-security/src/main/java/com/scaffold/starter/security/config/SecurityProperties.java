package com.scaffold.starter.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全部配置项，前缀 {@code scaffold.security}。
 *
 * <p>三条安全链与过滤器的白名单都从这里读取，是白名单的唯一出处，杜绝多处配置漂移；
 * token TTL、续期阈值、同端互斥等会话行为也集中在此调参。</p>
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "scaffold.security")
public class SecurityProperties {

    /** internal 链白名单（如登录接口），相对 /internal/** */
    private List<String> internalWhitelist = new ArrayList<>();

    /** openapi 链白名单 */
    private List<String> openapiWhitelist = new ArrayList<>();

    /** online 链白名单（整链默认 permitAll，此配置仅用于过滤器跳过解析） */
    private List<String> onlineWhitelist = new ArrayList<>();

    /** token 会话 TTL（秒），默认 7 天 */
    private long tokenTtlSeconds = 7 * 24 * 3600L;

    /** 剩余有效期低于该百分比时自动续期 */
    private int renewThresholdPercent = 20;

    /** 同端互斥：登录是否踢掉同设备旧会话 */
    private boolean exclusiveLogin = false;

    /** openapi 时间戳窗口（秒，正负） */
    private long openapiTimestampWindowSeconds = 300;

    /** openapi 是否校验来源 IP 白名单（需 AppIdentity 提供） */
    private boolean openapiIpWhitelistCheck = false;

    /** 密码策略（JeeSite 密码安全思想：强度校验 + 连续失败锁定） */
    private PasswordPolicy passwordPolicy = new PasswordPolicy();

    /** 登录 IP 防护（防单 IP 撞库，与账号维度锁定互补） */
    private IpGuard ipGuard = new IpGuard();

    /** internal 登录图形验证码 */
    private Captcha captcha = new Captcha();

    public static class IpGuard {

        /** 总开关；关闭后仅保留账号维度失败锁定 */
        private boolean enabled = true;

        /** 同 IP 窗口内登录失败上限，达到后拒绝该 IP 的账密登录 */
        private int failLimit = 30;

        /** 失败计数窗口（秒） */
        private int windowSeconds = 600;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getFailLimit() {
            return failLimit;
        }

        public void setFailLimit(int failLimit) {
            this.failLimit = failLimit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }

    public static class Captcha {

        /** 模式：off=不启用 | always=每次都要 | after_fail=连续失败 failThreshold 次后强制 */
        private String mode = "after_fail";

        /** after_fail 模式的触发阈值（连续失败次数） */
        private int failThreshold = 3;

        /** 验证码有效期（秒） */
        private int ttlSeconds = 120;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getFailThreshold() {
            return failThreshold;
        }

        public void setFailThreshold(int failThreshold) {
            this.failThreshold = failThreshold;
        }

        public int getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class PasswordPolicy {

        /** 连续登录失败次数上限，达到即临时锁定 */
        private int maxFailCount = 5;

        /** 锁定时长（秒） */
        private int lockSeconds = 900;

        /** 密码最小长度 */
        private int minLength = 8;

        /** 是否必须同时包含字母和数字 */
        private boolean requireLetterAndDigit = true;

        public int getMaxFailCount() {
            return maxFailCount;
        }

        public void setMaxFailCount(int maxFailCount) {
            this.maxFailCount = maxFailCount;
        }

        public int getLockSeconds() {
            return lockSeconds;
        }

        public void setLockSeconds(int lockSeconds) {
            this.lockSeconds = lockSeconds;
        }

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public boolean isRequireLetterAndDigit() {
            return requireLetterAndDigit;
        }

        public void setRequireLetterAndDigit(boolean requireLetterAndDigit) {
            this.requireLetterAndDigit = requireLetterAndDigit;
        }
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public IpGuard getIpGuard() {
        return ipGuard;
    }

    public void setIpGuard(IpGuard ipGuard) {
        this.ipGuard = ipGuard;
    }

    public Captcha getCaptcha() {
        return captcha;
    }

    public void setCaptcha(Captcha captcha) {
        this.captcha = captcha;
    }

    public List<String> getInternalWhitelist() {
        return internalWhitelist;
    }

    public void setInternalWhitelist(List<String> internalWhitelist) {
        this.internalWhitelist = internalWhitelist;
    }

    public List<String> getOpenapiWhitelist() {
        return openapiWhitelist;
    }

    public void setOpenapiWhitelist(List<String> openapiWhitelist) {
        this.openapiWhitelist = openapiWhitelist;
    }

    public List<String> getOnlineWhitelist() {
        return onlineWhitelist;
    }

    public void setOnlineWhitelist(List<String> onlineWhitelist) {
        this.onlineWhitelist = onlineWhitelist;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public int getRenewThresholdPercent() {
        return renewThresholdPercent;
    }

    public void setRenewThresholdPercent(int renewThresholdPercent) {
        this.renewThresholdPercent = renewThresholdPercent;
    }

    public boolean isExclusiveLogin() {
        return exclusiveLogin;
    }

    public void setExclusiveLogin(boolean exclusiveLogin) {
        this.exclusiveLogin = exclusiveLogin;
    }

    public long getOpenapiTimestampWindowSeconds() {
        return openapiTimestampWindowSeconds;
    }

    public void setOpenapiTimestampWindowSeconds(long openapiTimestampWindowSeconds) {
        this.openapiTimestampWindowSeconds = openapiTimestampWindowSeconds;
    }

    public boolean isOpenapiIpWhitelistCheck() {
        return openapiIpWhitelistCheck;
    }

    public void setOpenapiIpWhitelistCheck(boolean openapiIpWhitelistCheck) {
        this.openapiIpWhitelistCheck = openapiIpWhitelistCheck;
    }
}
