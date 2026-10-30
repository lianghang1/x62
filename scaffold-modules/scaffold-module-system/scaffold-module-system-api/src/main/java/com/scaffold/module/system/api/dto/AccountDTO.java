package com.scaffold.module.system.api.dto;

import java.io.Serializable;

/**
 * 统一账户 DTO，跨模块数据契约。
 *
 * <p>作为 system 模块对外的账户数据载体：其他业务模块只依赖本 DTO，
 * 不感知 system-biz 的实体与实现细节；字段按「账户类型 + 状态 + 展示名」
 * 抽象，兼容会员、运营者、应用三类账户。</p>
 *
 * @author lianghang
 */
public class AccountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 账户编号。 */
    private String accountNo;

    /** 账户类型：{@code MEMBER / OPERATOR / APP}。 */
    private String type;

    /** 账户状态：{@code ENABLE / DISABLE}。 */
    private String status;

    /** 展示名：昵称 / 姓名 / 应用名。 */
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
