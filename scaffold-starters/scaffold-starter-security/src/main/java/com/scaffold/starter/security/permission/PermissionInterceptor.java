package com.scaffold.starter.security.permission;

import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.identity.LoginUser;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 权限校验拦截器：进入 Controller 前校验 {@link RequirePermission} 声明的权限点。
 *
 * <p>执行位置在认证过滤器之后——{@code LoginContext} 已就绪，直接取当前用户比对；
 * 未登录（匿名）抛 401，已登录但无权抛 403，均走全局异常处理器的统一响应结构。
 * 无注解的接口零开销直通——权限校验只对显式声明的管理接口生效。</p>
 *
 * @author lianghang
 */
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        RequirePermission required = ((HandlerMethod) handler).getMethodAnnotation(RequirePermission.class);
        if (required == null) {
            required = ((HandlerMethod) handler).getBeanType().getAnnotation(RequirePermission.class);
        }
        if (required == null) {
            return true;
        }
        LoginUser user = LoginContext.currentOrNull();
        if (user == null) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED);
        }
        for (String permission : required.value()) {
            if (user.hasPermission(permission)) {
                return true;
            }
        }
        throw BizException.of(GlobalErrorCode.FORBIDDEN, "权限不足，需要："
                + Arrays.stream(required.value()).collect(Collectors.joining(" 或 ")));
    }
}
