package com.scaffold.search.plan.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询计划主表声明。
 *
 * @author lianghang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {

    /** 表名 */
    String name();

    /** 别名（字段映射默认前缀） */
    String alias() default "";

    /** 主键字段名 */
    String pkColumn() default "id";

    /** 强制索引名称（MySQL 语法：表名后拼 FORCE INDEX(idx)） */
    String useIndex() default "";

    /** 固定过滤条件（总是生效，前端无法覆盖） */
    SearchFilter[] filters() default {};
}
