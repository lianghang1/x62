package com.scaffold.search.plan.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询计划关联表声明，可重复标注实现多表联查。
 *
 * @author lianghang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(JoinTables.class)
public @interface JoinTable {

    /** 表名 */
    String name();

    /** 别名 */
    String alias();

    /** 关联字段 */
    JoinColumn[] columns() default {};

    /** 连接类型：left join / inner join / right join */
    String joinType() default "left join";

    /** 强制索引名称（MySQL 语法：表名后拼 FORCE INDEX(idx)） */
    String useIndex() default "";

    /** 数据库 schema（设置后表名拼为 schema.table） */
    String schema() default "";
}
