package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;
import com.scaffold.starter.security.identity.AccountType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * 统一账户表：三类入口殊途同归落在这里。
 */
@Entity
@Table(name = "sys_account")
public class SysAccount extends BaseEntity {

    @Column(name = "account_no", length = 64, nullable = false, unique = true)
    private String accountNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 16, nullable = false)
    private AccountType type;

    /** ENABLE / DISABLE */
    @Column(name = "status", length = 16, nullable = false)
    private String status = "ENABLE";

    /** OPERATOR 的 BCrypt 密码；MEMBER/APP 为空 */
    @Column(name = "password")
    private String password;

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
