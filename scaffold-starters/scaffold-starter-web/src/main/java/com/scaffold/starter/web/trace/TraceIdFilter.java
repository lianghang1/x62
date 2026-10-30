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
 * traceId 全链路过滤器，保证一次请求的所有日志可被同一标识串联。
 *
 * <p>处理顺序：优先复用上游透传的 {@code X-Trace-Id}（网关/前置服务生成，超长视为
 * 不可信重新生成），否则生成 32 位 UUID；随后写入 MDC（logback 以 {@code %X{traceId}}
 * 输出）与响应头，调用方报障时可直接提供。finally 强制清理——线程池复用下的
 * MDC 残留会导致日志串链路。</p>
 *
 * @author lianghang
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
