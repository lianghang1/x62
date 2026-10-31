package com.scaffold.search.plan.annotation;

import com.scaffold.search.filter.Operator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询计划主表声明（对齐 db-spring @Table）。
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

    /** 固定过滤条件（总是生效，前端无法覆盖） */
    Fixed[] filters() default {};

    /**
     * 固定过滤条件。
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface Fixed {

        /** 属性名（计划类字段名） */
        String name();

        /** 运算符 */
        Operator op() default Operator.EQ;

        /** 固定值（字符串，按 type 解析） */
        String value() default "";

        /** 值类型 */
        Class<?> type() default String.class;
    }
}
