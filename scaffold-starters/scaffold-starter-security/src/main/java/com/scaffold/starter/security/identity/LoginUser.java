package com.scaffold.starter.security.identity;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * 三条安全链认证成功后归一化的登录用户模型。
 *
 * <p>OPERATOR 的 roles/permissions 来自 RBAC；APP 的 scopes 并入 permissions；
 * MEMBER 默认无角色，资源归属校验由业务层按 accountId 过滤——身份与授权解耦，链路只负责认人。</p>
 *
 * @author lianghang
 */
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accountId;

    private String accountNo;

    private AccountType accountType;

    /** 登录名/应用名，用于日志与审计展示 */
    private String displayName;

    /** 角色（OPERATOR）或 scope（APP），已带 ROLE_ 前缀的角色存 roles，纯权限串存 permissions */
    private Set<String> roles = Collections.emptySet();

    private Set<String> permissions = Collections.emptySet();

    /** 登录设备/来源：internal / online / openapi:{appKey} */
    private String device;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role.startsWith("ROLE_") ? role : "ROLE_" + role);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
