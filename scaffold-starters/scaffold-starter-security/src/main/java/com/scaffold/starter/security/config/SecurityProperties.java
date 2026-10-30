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
