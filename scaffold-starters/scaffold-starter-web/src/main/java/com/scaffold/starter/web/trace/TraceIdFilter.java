package com.scaffold.starter.web.trace;

import com.scaffold.common.constant.TraceConstants;
import com.scaffold.common.result.R;
import com.scaffold.common.util.IpUtils;
import com.scaffold.starter.ip.IpRegionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>traceId 就绪后输出访问日志 START 行，作为请求到达性证明：与
 * {@code AccessLogAspect} 的 END 行靠 traceId 配对，有 START 无 END 即请求
 * 中断（进程被杀/超时/Filter 链异常）的审计信号。START 行任何异常都不影响请求。</p>
 *
 * @author lianghang
 */
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    public static final String TRACE_ID_HEADER = TraceConstants.TRACE_ID_HEADER;

    /** 是否输出访问日志 START 行。 */
    private final boolean accessStartLineEnabled;

    public TraceIdFilter() {
        this(true);
    }

    public TraceIdFilter(boolean accessStartLineEnabled) {
        this.accessStartLineEnabled = accessStartLineEnabled;
    }

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
            logAccessStart(request);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(R.TRACE_ID_KEY);
        }
    }

    /** 访问日志 START 行：请求到达性证明。失败静默——留痕不能反噬请求。 */
    private void logAccessStart(HttpServletRequest request) {
        if (!accessStartLineEnabled) {
            return;
        }
        try {
            String ip = IpUtils.clientIp(request);
            String query = request.getQueryString();
            String uri = query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
            log.info("[access] START {} {} {}({})", request.getMethod(), uri, ip, IpRegionUtil.getRegion(ip));
        } catch (Throwable t) {
            log.debug("[access] START 行记录失败，不影响请求", t);
        }
    }
}
