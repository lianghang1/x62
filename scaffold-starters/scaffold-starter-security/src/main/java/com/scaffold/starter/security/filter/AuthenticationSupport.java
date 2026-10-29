package com.scaffold.starter.security.filter;

import com.scaffold.starter.security.identity.LoginContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 过滤器公共逻辑：会话 -> LoginContext + SecurityContext 的装配与清理。
 */
final class AuthenticationSupport {

    private AuthenticationSupport() {
    }

    static void apply(com.scaffold.starter.security.token.SessionInfo session) {
        com.scaffold.starter.security.identity.LoginUser user = session.toLoginUser();
        LoginContext.set(user);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Set<String> roles = user.getRoles();
        if (roles != null) {
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority(
                        role.startsWith("ROLE_") ? role : "ROLE_" + role));
            }
        }
        Set<String> permissions = user.getPermissions();
        if (permissions != null) {
            for (String p : permissions) {
                authorities.add(new SimpleGrantedAuthority(p));
            }
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);
        authentication.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    static void clear() {
        LoginContext.clear();
        SecurityContextHolder.clearContext();
    }

    static void writeJson(javax.servlet.http.HttpServletResponse response,
                          com.scaffold.core.exception.ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus() == -1 ? 200 : errorCode.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.getWriter().write(
                "{\"code\":" + errorCode.getCode() + ",\"message\":\"" + errorCode.getMessage()
                        + "\",\"traceId\":\"" + org.slf4j.MDC.get("traceId") + "\"}");
        response.getWriter().flush();
    }

    static String extractToken(javax.servlet.http.HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return request.getHeader("X-Token");
    }
}
