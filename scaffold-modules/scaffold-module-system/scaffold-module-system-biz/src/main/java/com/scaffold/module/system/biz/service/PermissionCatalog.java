package com.scaffold.module.system.biz.service;

import java.util.Arrays;
import java.util.List;

/**
 * 系统权限点目录：全部可分配权限点的唯一登记处。
 *
 * <p>角色编辑界面的树形/勾选数据源由此提供——新增权限点时先在此登记，
 * 再到对应接口标注 {@code @RequirePermission}，两处权限串必须一致。
 * 内置超级管理员持有 {@code *} 全量权限，不在本目录列出。</p>
 *
 * @author lianghang
 */
public final class PermissionCatalog {

    /** 权限点定义：编码即校验用串，名称供界面展示。 */
    public static class Item {

        private final String code;
        private final String name;
        private final String module;

        public Item(String code, String name, String module) {
            this.code = code;
            this.name = name;
            this.module = module;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getModule() {
            return module;
        }
    }

    /** 全部权限点（按模块分组排序，展示顺序即列表顺序）。 */
    public static final List<Item> ALL = Arrays.asList(
            new Item("system:account:manage", "账户管理", "系统"),
            new Item("system:role:manage", "角色管理", "系统"),
            new Item("system:app:manage", "三方应用管理", "系统"),
            new Item("system:job:manage", "任务管理", "系统"),
            new Item("system:config:manage", "参数配置管理", "系统"),
            new Item("system:dict:manage", "字典管理", "系统"),
            new Item("system:audit:query", "审计日志查询", "审计"),
            new Item("system:online:query", "在线用户查询", "审计"));

    private PermissionCatalog() {
    }
}
