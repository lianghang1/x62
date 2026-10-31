package com.scaffold.search.plan.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略属性：未标注 @Column 的字段默认自动映射
 * （驼峰转下划线 + 主表别名前缀），加此注解的字段不参与 SQL 生成。
 *
 * @author lianghang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreProperty {

    /** 属性名称（与所在字段保持一致即可） */
    String name() default "";
}
