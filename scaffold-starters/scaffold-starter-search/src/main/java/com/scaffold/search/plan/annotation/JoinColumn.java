package com.scaffold.search.plan.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 关联字段声明。
 *
 * @author lianghang
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JoinColumn {

    /** 本表（关联表）字段名 */
    String name() default "";

    /** 主表关联字段（默认主键 id） */
    String referencedColumnName() default "id";

    /**
     * 自定义 ON 条件 SQL（设置后忽略 name/referencedColumnName/referencedValue）。
     * 例：{@code "d.dept_id = u.dept_id AND d.tenant_id = u.tenant_id"}
     */
    String sql() default "";

    /** 主表关联字段固定值（与 referencedColumnName 二选一） */
    String referencedValue() default "";
}
