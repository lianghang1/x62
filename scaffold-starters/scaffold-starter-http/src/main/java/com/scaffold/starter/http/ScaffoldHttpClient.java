package com.scaffold.starter.http;

import com.scaffold.common.constant.TraceConstants;
import com.scaffold.common.util.JsonUtils;
import com.scaffold.starter.http.sign.RequestSigner;
import com.scaffold.starter.http.transport.HttpTransport;
import com.scaffold.starter.http.transport.TransportException;
import com.scaffold.starter.http.transport.TransportRequest;
import com.scaffold.starter.http.transport.TransportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务侧唯一的 HTTP 出站门面：组装请求、透传 traceId、应用签名、执行重试。
 *
 * <p>规约落地：请求自动携带 {@code X-Trace-Id}（取自 MDC，跨系统排障）；
 * 幂等方法按配置自动重试（网络类失败），非幂等方法一次发送；
 * 失败统一抛 {@link com.scaffold.starter.http.transport.TransportException}。
 * 【禁止在数据库事务内调用】——事务内等待三方响应会长时间占用连接池。</p>
 *
 * @author lianghang
 */
public class ScaffoldHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ScaffoldHttpClient.class);

    /** 跨系统链路透传头 */
    public static final String TRACE_ID_HEADER = TraceConstants.TRACE_ID_HEADER;

    private final HttpTransport transport;
    private final HttpProperties properties;
    private final RequestSigner signer;

    public ScaffoldHttpClient(HttpTransport transport, HttpProperties properties, RequestSigner signer) {
        this.transport = transport;
        this.properties = properties;
        this.signer = signer;
    }

    /** GET 请求（幂等，可重试）。 */
    public TransportResponse get(String url) {
        return execute("GET", url, null, null);
    }

    /** POST JSON 请求体。 */
    public TransportResponse postJson(String url, Object body) {
        return execute("POST", url, body == null ? new byte[0] : JsonUtils.toJsonBytes(body),
                "application/json");
    }

    /** POST 原始字节（表单需自行编码）。 */
    public TransportResponse postBytes(String url, byte[] body, String contentType) {
        return execute("POST", url, body, contentType);
    }

    /** PUT JSON 请求体（幂等，可重试）。 */
    public TransportResponse putJson(String url, Object body) {
        return execute("PUT", url, body == null ? new byte[0] : JsonUtils.toJsonBytes(body),
                "application/json");
    }

    /** DELETE 请求（幂等，可重试）。 */
    public TransportResponse delete(String url) {
        return execute("DELETE", url, null, null);
    }

    /**
     * 统一执行入口：组请求 → traceId → 签名 → 幂等重试。
     *
     * @param method      HTTP 方法
     * @param url         完整地址
     * @param body        请求体（GET 传 null）
     * @param contentType 请求体类型（GET 传 null）
     */
    public TransportResponse execute(String method, String url, byte[] body, String contentType) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        String traceId = MDC.get(TraceConstants.TRACE_ID_KEY);
        if (traceId != null) {
            headers.put(TRACE_ID_HEADER, traceId);
            // W3C Trace Context：对接 OTel/云厂商体系时链路不断
            headers.put(TraceConstants.TRACEPARENT_HEADER,
                    "00-" + traceId + "-"
                            + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "-01");
        }
        TransportRequest request = new TransportRequest(method, url, headers, body, contentType,
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
        if (signer != null) {
            request = signer.sign(request);
        }

        int maxAttempts = isIdempotent(method) ? properties.getRetryMaxAttempts() : 1;
        long backoffMs = properties.getRetryBackoffMs();
        TransportException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long start = System.currentTimeMillis();
            try {
                TransportResponse response = transport.execute(request);
                if (log.isDebugEnabled()) {
                    log.debug("[http] {} {} -> {} ({}ms)", method, url, response.getStatus(),
                            System.currentTimeMillis() - start);
                } else if (attempt > 1 || !response.isSuccessful()) {
                    log.info("[http] {} {} -> {} ({}ms)", method, url, response.getStatus(),
                            System.currentTimeMillis() - start);
                }
                return response;
            } catch (TransportException e) {
                lastError = e;
                if (attempt < maxAttempts && retryable(e)) {
                    log.warn("[http] {} {} 第 {} 次失败（{}），{}ms 后重试",
                            method, url, attempt, e.getErrorType(), backoffMs);
                    sleepQuietly(backoffMs);
                    continue;
                }
                log.error("[http] {} {} 最终失败（{}）", method, url, e.getErrorType(), e);
                throw e;
            }
        }
        throw lastError;
    }

    /** GET/PUT/DELETE 视为幂等可重试，POST 默认不重试（防重复下单）。 */
    private static boolean isIdempotent(String method) {
        return "GET".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method);
    }

    /** 仅网络类失败重试；中断立即放弃（线程要退出）。 */
    private static boolean retryable(TransportException e) {
        return e.getErrorType() != com.scaffold.starter.http.transport.TransportErrorType.INTERRUPTED
                && e.getErrorType() != com.scaffold.starter.http.transport.TransportErrorType.CONNECT;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new com.scaffold.starter.http.transport.TransportException(
                    com.scaffold.starter.http.transport.TransportErrorType.INTERRUPTED,
                    "HTTP 重试等待被中断", e);
        }
    }

    /** 便捷方法：POST JSON 并按 UTF-8 返回响应体。 */
    public String postJsonForString(String url, Object body) {
        return new String(postJson(url, body).getBody(), StandardCharsets.UTF_8);
    }
}
