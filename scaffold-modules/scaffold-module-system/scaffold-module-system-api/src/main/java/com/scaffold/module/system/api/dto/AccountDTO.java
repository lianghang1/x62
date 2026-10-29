package com.scaffold.module.system.api.dto;

import java.io.Serializable;

/**
 * 统一账户 DTO（跨模块契约）。
 */
public class AccountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 账户编号 */
    private String accountNo;

    /** MEMBER / OPERATOR / APP */
    private String type;

    /** ENABLE / DISABLE */
    private String status;

    /** 展示名：昵称 / 姓名 / 应用名 */
    private String displayName;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
