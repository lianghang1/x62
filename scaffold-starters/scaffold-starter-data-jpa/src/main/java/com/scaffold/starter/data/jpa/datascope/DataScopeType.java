package com.scaffold.starter.data.jpa.datascope;

/**
 * 数据权限范围类型。
 *
 * <p>当前提供两档：ALL（不做过滤）与 SELF（仅本人数据，按 create_by 匹配当前审计人）。
 * 部门等组织维度依赖组织实体模型，待业务层落地后以同样的 FilterDef + 切面方式扩展，
 * 届时只需新增 FilterDef 与类型枚举，本组件的使用方式不变。</p>
 *
 * @author lianghang
 */
public enum DataScopeType {

    /** 不过滤，行为与无注解一致。 */
    ALL,

    /** 仅本人：查询自动追加 create_by = 当前审计人（来自 ScaffoldAuditorHolder）。 */
    SELF
}
