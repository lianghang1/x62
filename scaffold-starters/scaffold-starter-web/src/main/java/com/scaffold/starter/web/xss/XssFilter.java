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
 * XSS 过滤器：默认对所有请求做参数与 body 尖括号转义；
 * scaffold.web.xss.exclude-urls 可按 Ant 风格排除（如富文本编辑接口）。
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
