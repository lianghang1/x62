package com.scaffold.search.plan.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命名查询计划（对齐 db-spring @QueryPlan）。
 *
 * <p>标注在联表查询结构类上，配合 {@link Table} / {@link JoinTable} /
 * {@link Column} 声明联表结构与字段映射；运行时经
 * {@code ScaffoldMetaResolver} 合成为 Bean Searcher 元数据，SQL 全部由
 * Bean Searcher 生成。
 *
 * <pre>{@code
 * @QueryPlan(name = "userPlan", returnType = UserPlanResult.class)
 * @Table(name = "t_user", alias = "u")
 * @JoinTable(name = "t_dept", alias = "d",
 *         columns = @JoinColumn(name = "dept_id", referencedColumnName = "id"))
 * public class UserPlan {
 *     @Column("u.name") private String name;
 *     @Column("d.dept_name") private String deptName;
 * }
 *
 * QueryUtils.doQuery("userPlan", filters, pageable);   // 按名执行
 * QueryUtils.doQuery(UserPlan.class, filters, pageable); // 按类执行
 * }</pre>
 *
 * <p>{@code returnType}：结果封装类；缺省（Void）时直接返回计划类实例。
 * 两者不同时，结果按属性名自动转换（Map → returnType）。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryPlan {

    /** 计划名称，注册到 QueryPlanRegistry，供按名查找 */
    String name() default "";

    /** 返回数据封装 Bean，缺省为 Void（直接返回计划类实例） */
    Class<?> returnType() default Void.class;
}
