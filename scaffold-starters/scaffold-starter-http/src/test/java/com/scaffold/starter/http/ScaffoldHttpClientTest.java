package com.scaffold.starter.http;

import com.scaffold.starter.http.transport.OkHttpTransport;
import com.scaffold.starter.http.transport.TransportResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTTP 门面行为测试：MockWebServer 模拟三方端点。
 *
 * <p>重点锁四条契约：traceId 出站透传、GET 网络抖动自动重试、
 * POST 不重试防重复下单、非 2xx 原样返回不抛异常（业务码由调用方解析）。</p>
 *
 * @author lianghang
 */
class ScaffoldHttpClientTest {

    private MockWebServer server;
    private HttpProperties properties;
    private ScaffoldHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        // 显式绑定 IPv4：localhost 双栈解析会导致 okhttp 重试时漂移到 ::1
        server.start(java.net.InetAddress.getByName("127.0.0.1"), 0);
        properties = new HttpProperties();
        properties.setRetryBackoffMs(10);
        client = new ScaffoldHttpClient(
                new OkHttpTransport(properties.getConnectTimeoutMs(), properties.getReadTimeoutMs()),
                properties, null);
    }

    /** 统一用 127.0.0.1 构造 URL，规避 localhost 双栈解析问题。 */
    private String url(String path) {
        return "http://127.0.0.1:" + server.getPort() + path;
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getCarriesTraceIdAndParsesResponse() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        org.slf4j.MDC.put("traceId", "trace-abc-123");

        TransportResponse response = client.get(url("/api/demo"));

        assertEquals(200, response.getStatus());
        assertTrue(response.bodyAsString().contains("ok"));
        String traceHeader = server.takeRequest().getHeader("X-Trace-Id");
        assertEquals("trace-abc-123", traceHeader);
        org.slf4j.MDC.clear();
    }

    @Test
    void getRetriesOnceOnNetworkDropThenSucceeds() throws InterruptedException {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        server.enqueue(new MockResponse().setBody("recovered"));

        TransportResponse response = client.get(url("/api/retry"));

        assertEquals(200, response.getStatus());
        assertEquals("recovered", response.bodyAsString());
        assertEquals(2, server.getRequestCount());
    }

    @Test
    void postIsNeverRetriedEvenOnNetworkDrop() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        server.enqueue(new MockResponse().setBody("should not reach"));

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("amount", 1);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.scaffold.starter.http.transport.TransportException.class,
                () -> client.postJson(url("/api/order"), payload));
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void non2xxReturnedAsIsWithoutException() {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"code\":1404}"));

        TransportResponse response = client.get(url("/api/missing"));

        assertEquals(404, response.getStatus());
        assertTrue(response.bodyAsString().contains("1404"));
    }

    @Test
    void timeoutSurfacesAsTransportException() throws IOException {
        MockWebServer slowServer = new MockWebServer();
        slowServer.start();
        slowServer.enqueue(new MockResponse().setBody("late").setBodyDelay(3, java.util.concurrent.TimeUnit.SECONDS));
        properties.setReadTimeoutMs(200);
        properties.setRetryMaxAttempts(1);
        ScaffoldHttpClient strictClient = new ScaffoldHttpClient(
                new OkHttpTransport(properties.getConnectTimeoutMs(), properties.getReadTimeoutMs()),
                properties, null);

        com.scaffold.starter.http.transport.TransportException ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.scaffold.starter.http.transport.TransportException.class,
                        () -> strictClient.get(url("/api/slow")));
        assertEquals(com.scaffold.starter.http.transport.TransportErrorType.TIMEOUT, ex.getErrorType());
        slowServer.shutdown();
    }

    @Test
    void signerHeadersApplied() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("ok"));
        ScaffoldHttpClient signedClient = new ScaffoldHttpClient(
                new OkHttpTransport(properties.getConnectTimeoutMs(), properties.getReadTimeoutMs()),
                properties,
                new com.scaffold.starter.http.sign.HmacRequestSigner("demo-app", "secret"));

        signedClient.get(url("/api/signed"));

        okhttp3.mockwebserver.RecordedRequest request = server.takeRequest();
        assertEquals("demo-app", request.getHeader("X-Sign-App-Key"));
        org.junit.jupiter.api.Assertions.assertNotNull(request.getHeader("X-Sign-Signature"));
        org.junit.jupiter.api.Assertions.assertNotNull(request.getHeader("X-Sign-Timestamp"));
    }

    @Test
    void postJsonSerializesBody() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("created"));
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("name", "订单A");

        client.postJson(url("/api/create"), payload);

        assertEquals("{\"name\":\"订单A\"}", server.takeRequest().getBody().readUtf8());
    }
}
