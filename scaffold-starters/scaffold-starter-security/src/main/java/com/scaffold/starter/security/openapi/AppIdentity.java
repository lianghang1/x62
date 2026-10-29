package com.scaffold.starter.security.openapi;

import java.util.Collections;
import java.util.Set;

/**
 * openapi 三方应用身份（由业务方提供实现：module-system 按 appKey 查 app_info）。
 */
public class AppIdentity {

    /** sys_account 账户 ID（APP 类型） */
    private Long accountId;

    private String appKey;

    private String appSecret;

    private String appName;

    /** 授权范围（scope 列表），签发到会话的 permissions */
    private Set<String> scopes = Collections.emptySet();

    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }
}
