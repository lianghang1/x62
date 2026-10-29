package com.scaffold.module.system.biz.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 账户-角色关联（纯关联表，无审计诉求，用简单主键）。
 */
@Entity
@Table(name = "sys_account_role")
public class AccountRole {

    @Id
    @GeneratedValue(generator = "snowflake")
    @org.hibernate.annotations.GenericGenerator(name = "snowflake",
            strategy = "com.scaffold.starter.data.jpa.id.SnowflakeIdentifierGenerator")
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
