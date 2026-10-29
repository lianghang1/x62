package com.scaffold.starter.security.filter;

import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.starter.security.config.SecurityProperties;
import com.scaffold.starter.security.token.SessionInfo;
import com.scaffold.starter.security.token.TokenService;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * internal 链认证过滤器：token 会话 -> LoginContext。
 * 白名单路径跳过；finally 强制清理上下文，防线程复用串号。
 */
public class InternalAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final SecurityProperties properties;

    public InternalAuthenticationFilter(TokenService tokenService, SecurityProperties properties) {
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return properties.getInternalWhitelist().contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String token = AuthenticationSupport.extractToken(request);
            SessionInfo session = tokenService.resolveWithRenew(token);
            if (session == null) {
                AuthenticationSupport.writeJson(response, GlobalErrorCode.UNAUTHORIZED);
                return;
            }
            AuthenticationSupport.apply(session);
            chain.doFilter(request, response);
        } finally {
            AuthenticationSupport.clear();
        }
    }
}
