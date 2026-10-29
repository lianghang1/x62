package com.scaffold.starter.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全部配置（前缀 scaffold.security）。白名单唯一出处：安全链与过滤器都从这里读，杜绝漂移。
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
