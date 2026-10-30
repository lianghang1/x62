package com.scaffold.starter.http.transport;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * 出站 HTTP 响应的传输层抽象。
 *
 * <p>不解析业务语义：状态码与响应体原样交给调用方判定——
 * 三方接口「200 包错误码」的惯例由业务层处理，transport 不越权。</p>
 *
 * @author lianghang
 */
public class TransportResponse {

    private final int status;
    private final Map<String, String> headers;
    private final byte[] body;

    public TransportResponse(int status, Map<String, String> headers, byte[] body) {
        this.status = status;
        this.headers = headers == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(headers);
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    /** 响应体按 UTF-8 转字符串（日志与调试便捷方法）。 */
    public String bodyAsString() {
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }

    /** 2xx 判定（transport 层的成功语义，业务码仍由调用方解析）。 */
    public boolean isSuccessful() {
        return status >= 200 && status < 300;
    }
}
