package com.scaffold.starter.security.permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * internal 管理接口权限声明：标注在方法或类上，由 {@link PermissionInterceptor}
 * 在进入业务前校验当前登录用户的权限集。
 *
 * <p>权限串采用 {@code 模块:资源:动作} 风格（如 {@code system:account:manage}），
 * 支持段级通配 {@code system:*} 与全量 {@code *}（超级管理员）；多值语义为
 * <b>任一满足即放行</b>（any-of）。类级声明对该类全部接口生效，方法级覆盖类级。</p>
 *
 * @author lianghang
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 所需权限点（any-of：具备其中任意一个即放行）。 */
    String[] value();
}
