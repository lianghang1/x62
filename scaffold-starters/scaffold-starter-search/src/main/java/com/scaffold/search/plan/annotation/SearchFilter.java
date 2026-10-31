package com.scaffold.search.plan.annotation;

import com.scaffold.search.filter.Operator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 固定过滤条件注解（对齐 db-spring dynamic.annotation.SearchFilter：name/op/value/type）。
 *
 * <p>用于 {@link Table#filters()}，声明的条件总是生效，前端无法覆盖。
 * 注意：引用的字段必须在计划类中有 {@link Column} 映射。
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SearchFilter {

    /** 属性名（计划类字段名） */
    String name();

    /** 运算符 */
    Operator op() default Operator.EQ;

    /** 固定值（字符串，按 type 解析） */
    String value() default "";

    /** 值类型 */
    Class<?> type() default String.class;
}
