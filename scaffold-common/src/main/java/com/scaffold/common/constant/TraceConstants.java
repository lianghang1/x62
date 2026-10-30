package com.scaffold.common.constant;

/**
 * 全局追踪契约：traceId 的传递口径（HTTP 头、MDC key、响应体字段名）。
 *
 * <p>三处口径必须一致，统一在此收口——改动任意一端都要经本类引用，
 * 避免散落硬编码导致链路断点。</p>
 *
 * @author lianghang
 */
public final class TraceConstants {

    /** HTTP 请求/响应头：上游透传入口（网关/前置服务带 X-Trace-Id 进来则复用） */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** MDC key 与统一响应体 {@code R} 的 traceId 字段名（同一口径） */
    public static final String TRACE_ID_KEY = "traceId";

    private TraceConstants() {
    }
}
