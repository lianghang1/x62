package com.scaffold.starter.web.xss;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * XSS 过滤器，对所有请求的参数与 body 做尖括号转义。
 *
 * <p>以请求包装器方式实现，默认全站生效；{@code scaffold.web.xss.exclude-urls}
 * 支持按 Ant 风格排除（如富文本编辑接口）——排除名单走 {@code shouldNotFilter}，
 * 命中即放行原始请求。</p>
 *
 * @author lianghang
 */
public class XssFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final List<String> excludeUrls;

    public XssFilter(List<String> excludeUrls) {
        this.excludeUrls = excludeUrls;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (excludeUrls == null || excludeUrls.isEmpty()) {
            return false;
        }
        String uri = request.getRequestURI();
        for (String pattern : excludeUrls) {
            if (MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new XssRequestWrapper(request), response);
    }
}
