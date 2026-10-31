package com.scaffold.search.plan.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询计划主表声明（对齐 db-spring @Table）。
 *
 * <p>db-spring 的 {@code useIndex}（强制索引提示）未实现：跨数据库方言语义不同
 * （MySQL force index / Oracle hint），Bean Searcher 无对应概念；需要时可直接在
 * {@link Column#name()} 里写方言 hint 或自定义 tables。
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
    SearchFilter[] filters() default {};
}
