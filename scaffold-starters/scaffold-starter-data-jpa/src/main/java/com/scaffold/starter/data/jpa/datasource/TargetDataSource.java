package com.scaffold.starter.data.jpa.datasource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多数据源切换注解，标注在类或方法上，指明本次调用走哪个数据源。
 *
 * <p>key 必须与 {@code scaffold.datasource.dynamic.datasources} 中配置的数据源名一致，
 * 切面在进入方法前做 fail-fast 校验，配置缺失直接抛异常而非静默回落主库。</p>
 *
 * <p>方法与类同时标注时方法优先；两个数据源间切换必须在事务开启之前完成，
 * 切面已注册为最高优先级（先于事务拦截器执行）。同一事务内不可再切换——
 * 连接已在事务开始时绑定，此为 JDBC 事务模型决定的天然边界。</p>
 *
 * @author lianghang
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {

    /**
     * 目标数据源名，对应 {@code scaffold.datasource.dynamic.datasources.{name}}。
     */
    String value();
}
