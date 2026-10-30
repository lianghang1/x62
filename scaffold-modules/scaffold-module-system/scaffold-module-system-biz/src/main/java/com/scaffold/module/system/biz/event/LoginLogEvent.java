package com.scaffold.module.system.biz.event;

import java.time.LocalDateTime;

/**
 * 登录日志事件（成功与失败都发）。
 *
 * <p>由 {@code AuthService} 在请求线程内取齐 IP/UA 后经 {@code EventOps}
 * 发布，{@code AuditLogListener} 异步落库——登录主流程不再被写库拖慢，
 * 上下文在发布前取齐，异步线程无 RequestContextHolder 也可用。</p>
 *
 * @author lianghang
 */
public class LoginLogEvent {

    private final String accountNo;
    private final String loginType;
    private final boolean success;
    private final String failReason;
    private final String ip;
    private final String userAgent;
    private final LocalDateTime loginTime;

    public LoginLogEvent(String accountNo, String loginType, boolean success,
                         String failReason, String ip, String userAgent, LocalDateTime loginTime) {
        this.accountNo = accountNo;
        this.loginType = loginType;
        this.success = success;
        this.failReason = failReason;
        this.ip = ip;
        this.userAgent = userAgent;
        this.loginTime = loginTime;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public String getLoginType() {
        return loginType;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailReason() {
        return failReason;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }
}
