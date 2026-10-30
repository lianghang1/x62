package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 角色实体。
 *
 * <p>轻量 RBAC 的角色侧：角色持有一组权限串，账户经关联表获得角色；
 * 菜单级权限按需扩展 role_menu 表，本表结构保持最小可用。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_role")
public class SysRole extends BaseEntity {

    @Column(name = "role_code", length = 64, nullable = false, unique = true)
    private String code;

    @Column(name = "role_name", length = 64, nullable = false)
    private String name;

    /** 权限串集合，逗号分隔（如 {@code order:list,order:create}）。 */
    @Column(name = "permissions", length = 2048)
    private String permissions;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
