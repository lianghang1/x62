package com.scaffold.search.plan.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段类型声明（对齐 db-spring @FieldType）：覆盖某属性对应的数据类型，
 * 影响检索值的类型转换（映射到 Bean Searcher DbType）。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldType {

    /** 查询字段属性名称 */
    String name();

    /** 字段对应的数据类型 */
    Class<?> type();
}
