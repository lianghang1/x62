package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * APP 扩展信息：三方系统的凭证与授权范围。
 */
@Entity
@Table(name = "app_info")
public class AppInfo extends BaseEntity {

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "app_name", length = 64, nullable = false)
    private String appName;

    @Column(name = "app_key", length = 64, nullable = false, unique = true)
    private String appKey;

    /** 明文仅供创建时展示一次；库里存的是加密结果（demo 用可逆加密，生产建议 KMS） */
    @Column(name = "app_secret", length = 128, nullable = false)
    private String appSecret;

    /** scope 集合，逗号分隔 */
    @Column(name = "scopes", length = 512)
    private String scopes;

    /** 来源 IP 白名单，逗号分隔；空 = 不限制 */
    @Column(name = "ip_whitelist", length = 512)
    private String ipWhitelist;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(String ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }
}
