package com.scaffold.starter.http.transport;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 okhttp 的传输层默认实现。
 *
 * <p>每个请求应用 {@link TransportRequest} 自带的超时覆盖全局配置；
 * 连接池与客户端单例复用（由装配层持有），读超时按请求粒度可变。</p>
 *
 * @author lianghang
 */
public class OkHttpTransport implements HttpTransport {

    private final OkHttpClient client;

    public OkHttpTransport(int connectTimeoutMs, int readTimeoutMs) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    public OkHttpTransport(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public TransportResponse execute(TransportRequest request) {
        Request.Builder builder = new Request.Builder().url(request.getUrl());
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        if (request.hasBody()) {
            MediaType mediaType = MediaType.parse(
                    request.getContentType() == null ? "application/json" : request.getContentType());
            RequestBody body = RequestBody.create(mediaType, request.getBody());
            builder.method(request.getMethod(), body);
        } else if ("POST".equalsIgnoreCase(request.getMethod())
                || "PUT".equalsIgnoreCase(request.getMethod())
                || "PATCH".equalsIgnoreCase(request.getMethod())
                || "DELETE".equalsIgnoreCase(request.getMethod())) {
            builder.method(request.getMethod(), RequestBody.create((MediaType) null, new byte[0]));
        } else {
            builder.method(request.getMethod(), null);
        }

        OkHttpClient perRequest = request.getReadTimeoutMs() > 0
                ? client.newBuilder()
                        .connectTimeout(request.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                        .readTimeout(request.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                        .build()
                : client;

        try (Response response = perRequest.newCall(builder.build()).execute()) {
            byte[] body = response.body() == null ? new byte[0] : response.body().bytes();
            okhttp3.Headers headers = response.headers();
            java.util.Map<String, String> headerMap = new java.util.LinkedHashMap<String, String>();
            for (int i = 0; i < headers.size(); i++) {
                headerMap.put(headers.name(i), headers.value(i));
            }
            return new TransportResponse(response.code(), headerMap, body);
        } catch (IOException e) {
            throw mapError(e, request);
        }
    }

    /** 按异常特征归类为 TransportErrorType，供重试与告警决策。 */
    private static TransportException mapError(IOException e, TransportRequest request) {
        String message = "HTTP 调用失败: " + request.getMethod() + " " + request.getUrl()
                + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")";
        if (e instanceof java.net.ConnectException || e instanceof java.net.UnknownHostException
                || e instanceof java.net.NoRouteToHostException) {
            return new TransportException(TransportErrorType.CONNECT, message, e);
        }
        if (e instanceof java.net.SocketTimeoutException) {
            return new TransportException(TransportErrorType.TIMEOUT, message, e);
        }
        return new TransportException(TransportErrorType.RETRYABLE_IO, message, e);
    }
}
