package com.scaffold.module.system.biz.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 账户-角色关联表。
 *
 * <p>纯关联表，无审计诉求，故不继承 BaseEntity、直接用雪花简单主键；
 * 账户与角色为多对多，权限随角色授予。</p>
 *
 * @author lianghang
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
