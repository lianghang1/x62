package com.scaffold.starter.security.filter;

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
 * online 链的可选登录认证过滤器。
 *
 * <p>带有效 token 则归一化身份（获得增强能力），匿名照常放行——
 * C 端接口允许游客访问，登录与否只影响数据可见范围。</p>
 *
 * @author lianghang
 */
public class OnlineTokenFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final SecurityProperties properties;

    public OnlineTokenFilter(TokenService tokenService, SecurityProperties properties) {
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return properties.getOnlineWhitelist().contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String token = AuthenticationSupport.extractToken(request);
            if (token != null && !token.isEmpty()) {
                SessionInfo session = tokenService.resolveWithRenew(token);
                if (session != null) {
                    AuthenticationSupport.apply(session);
                }
            }
            chain.doFilter(request, response);
        } finally {
            AuthenticationSupport.clear();
        }
    }
}
