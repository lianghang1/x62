package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 角色（轻量 RBAC：角色 + 权限串；菜单级权限按需扩展 role_menu 表）。
 */
@Entity
@Table(name = "sys_role")
public class SysRole extends BaseEntity {

    @Column(name = "role_code", length = 64, nullable = false, unique = true)
    private String code;

    @Column(name = "role_name", length = 64, nullable = false)
    private String name;

    /** 权限串集合，逗号分隔（如 order:list,order:create） */
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
