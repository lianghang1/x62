package com.scaffold.starter.http.transport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 出站 HTTP 请求的传输层抽象，与具体客户端解耦（默认实现 okhttp）。
 *
 * <p>字段为不可变快照：{@link com.scaffold.starter.http.ScaffoldHttpClient} 组装请求
 * （含 traceId、签名），transport 只负责发送；需要扩展头部时用 {@link #withHeader}
 * 返回新实例，避免共享可变状态。</p>
 *
 * @author lianghang
 */
public class TransportRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final byte[] body;
    private final String contentType;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public TransportRequest(String method, String url, Map<String, String> headers,
                            byte[] body, String contentType,
                            int connectTimeoutMs, int readTimeoutMs) {
        this.method = method;
        this.url = url;
        this.headers = headers == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, String>(headers));
        this.body = body;
        this.contentType = contentType;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    /** 追加单个请求头（返回新实例，原实例不变）。 */
    public TransportRequest withHeader(String name, String value) {
        Map<String, String> merged = new LinkedHashMap<String, String>(this.headers);
        merged.put(name, value);
        return new TransportRequest(method, url, merged, body, contentType,
                connectTimeoutMs, readTimeoutMs);
    }

    /** 是否携带请求体（GET/HEAD 不带）。 */
    public boolean hasBody() {
        return body != null && body.length > 0;
    }
}
