package com.scaffold.starter.web.trace;

import com.scaffold.core.result.R;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * traceId 全链路过滤器：
 * <ol>
 *   <li>优先复用上游透传的 X-Trace-Id（网关/前置服务生成），否则生成 32 位 UUID；</li>
 *   <li>写入 MDC（logback 通过 %X{traceId} 输出）；</li>
 *   <li>写入响应头，调用方报障时可直接提供；</li>
 *   <li>finally 强制清理——线程池复用下的 MDC 残留会导致日志串链路。</li>
 * </ol>
 */
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.length() > 32) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(R.TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(R.TRACE_ID_KEY);
        }
    }
}
