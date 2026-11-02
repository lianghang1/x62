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

    /**
     * 系统任务身份工厂：定时任务、消息消费等无 HTTP 请求的后台线程使用。
     *
     * <p>accountId 固定 0（不入库的身份），accountType 取 OPERATOR（内部操作方），
     * permissions 为 {@code *} 全量——后台任务以系统身份执行业务时不受权限拦截，
     * 审计字段 createBy/updateBy 自动落 {@code system}。
     * 经 {@code LoginContext.set} 写入后，TTL 底座保证子线程同样可见。</p>
     *
     * @param operator 操作来源标识，如 job 名（log/审计展示用）
     */
    public static LoginUser system(String operator) {
        LoginUser user = new LoginUser();
        user.setAccountId(0L);
        user.setAccountNo("system");
        user.setAccountType(AccountType.OPERATOR);
        user.setDisplayName("system:" + (operator == null ? "task" : operator));
        user.setPermissions(Collections.singleton("*"));
        user.setDevice("job");
        return user;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role.startsWith("ROLE_") ? role : "ROLE_" + role);
    }

    /**
     * 权限判定：{@code *} 全量放行（超级管理员）；精确匹配；
     * 或持有 {@code system:account:*} 形式的段级通配。任一命中即放行。
     */
    public boolean hasPermission(String permission) {
        if (permissions == null || permission == null) {
            return false;
        }
        if (permissions.contains("*") || permissions.contains(permission)) {
            return true;
        }
        int wildcard = permission.lastIndexOf(':');
        while (wildcard > 0) {
            if (permissions.contains(permission.substring(0, wildcard) + ":*")) {
                return true;
            }
            wildcard = permission.lastIndexOf(':', wildcard - 1);
        }
        return false;
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
