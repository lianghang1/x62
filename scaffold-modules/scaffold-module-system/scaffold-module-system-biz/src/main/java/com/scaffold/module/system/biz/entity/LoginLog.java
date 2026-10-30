package com.scaffold.module.system.biz.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 登录日志表。
 *
 * <p>每次登录（成功或失败）记一行，是安全审计与失败锁定的依据；
 * 按账号与时间建索引以支撑高频的「查某人近况」「查某时段异常」查询。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_login_log", indexes = {
        @Index(name = "idx_login_log_account", columnList = "account_no"),
        @Index(name = "idx_login_log_time", columnList = "login_time")
})
public class LoginLog {

    @Id
    @GeneratedValue(generator = "snowflake")
    @org.hibernate.annotations.GenericGenerator(name = "snowflake",
            strategy = "com.scaffold.starter.data.jpa.id.SnowflakeIdentifierGenerator")
    private Long id;

    @Column(name = "account_no", length = 64)
    private String accountNo;

    /** 登录入口：{@code internal / online:wechat-mp} 等。 */
    @Column(name = "login_type", length = 32)
    private String loginType;

    @Column(name = "success", nullable = false)
    private Boolean success;

    /** 失败原因，成功时为空。 */
    @Column(name = "fail_reason", length = 128)
    private String failReason;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
}
