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
import java.util.regex.Pattern;

/**
 * traceId 全链路过滤器，保证一次请求的所有日志可被同一标识串联。
 *
 * <p>处理顺序：优先复用 W3C Trace Context（{@code traceparent}）或上游透传的
 * {@code X-Trace-Id}（网关/前置服务生成，超长/格式非法视为不可信重新生成），
 * 否则生成 32 位 UUID；随后写入 MDC（logback 以 {@code %X{traceId}}
 * 输出）与响应头（X-Trace-Id 与 traceparent 双写），调用方报障时可直接提供。
 * finally 强制清理——线程池复用下的 MDC 残留会导致日志串链路。</p>
 *
 * <p>traceId 就绪后输出访问日志 START 行，作为请求到达性证明：与
 * {@code AccessLogAspect} 的 END 行靠 traceId 配对，有 START 无 END 即请求
 * 中断（进程被杀/超时/Filter 链异常）的审计信号。START 行任何异常都不影响请求。</p>
 *
 * @author lianghang
 */
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    /** W3C 规范 traceId：32 位小写十六进制。 */
    private static final Pattern HEX_32 = Pattern.compile("^[0-9a-f]{32}$");

    public static final String TRACE_ID_HEADER = TraceConstants.TRACE_ID_HEADER;
    public static final String TRACEPARENT_HEADER = TraceConstants.TRACEPARENT_HEADER;

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
        String traceId = resolveTraceId(request);
        MDC.put(R.TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(TRACEPARENT_HEADER, toTraceparent(traceId));
        try {
            logAccessStart(request);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(R.TRACE_ID_KEY);
        }
    }

    /**
     * traceId 解析优先级：W3C traceparent → X-Trace-Id（网关/自家上游）→ 本地生成。
     * 超长/格式非法视为不可信，回退本地生成——外部输入永不直接进 MDC。
     */
    private String resolveTraceId(HttpServletRequest request) {
        String traceparent = request.getHeader(TRACEPARENT_HEADER);
        String fromTraceparent = traceparent == null ? null : traceIdOfTraceparent(traceparent);
        if (fromTraceparent != null) {
            return fromTraceparent;
        }
        String headerTraceId = request.getHeader(TRACE_ID_HEADER);
        if (headerTraceId != null && headerTraceId.length() <= 32) {
            return headerTraceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 从 {@code 00-<32位traceId>-<16位spanId>-<标志>} 中取 traceId 段，非法返回 null。 */
    private String traceIdOfTraceparent(String traceparent) {
        String[] parts = traceparent.trim().split("-");
        return parts.length == 4 && HEX_32.matcher(parts[1]).matches() ? parts[1] : null;
    }

    /** 以当前 traceId 组装 W3C traceparent（版本 00，随机 spanId，采样标志 01）。 */
    private String toTraceparent(String traceId) {
        return "00-" + traceId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "-01";
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
