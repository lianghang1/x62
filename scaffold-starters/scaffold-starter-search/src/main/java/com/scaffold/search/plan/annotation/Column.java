package com.scaffold.search.plan.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询计划字段映射（对齐 db-spring @Column）。
 *
 * <p>标注在计划类的属性上；未标注的属性不参与 SQL 生成。
 * name 缺省时使用属性名，table 缺省时使用主表别名。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column {

    /**
     * 数据库列表达式（含别名前缀，如 {@code "u.name"}、
     * 也可以是任意 SQL 片段如 {@code "count(o.id)"}）。
     * <p>等价于 {@link #name()}，支持 {@code @Column("u.id")} 简写。
     */
    String value() default "";

    /** 数据库列表达式，与 {@link #value()} 等价 */
    String name() default "";

    /** 表别名（name 未含前缀时使用），缺省为主表别名 */
    String table() default "";
}
