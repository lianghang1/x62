package com.scaffold.starter.security.filter;

import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.starter.security.config.SecurityProperties;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.openapi.AppIdentity;
import com.scaffold.starter.security.openapi.AppSecretProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * openapi 签名过滤器测试：头缺失/未知应用/时间窗越界/nonce 重放/签名不符逐项拒绝，
 * 合法签名放行并装配 APP 身份。
 *
 * @author lianghang
 */
class OpenApiSignFilterTest {

    private static final String APP_KEY = "app-1";
    private static final String APP_SECRET = "secret-1";
    private static final long NOW = System.currentTimeMillis();

    private AppSecretProvider secretProvider;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private SecurityProperties properties;
    private OpenApiSignFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        secretProvider = mock(AppSecretProvider.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        properties = new SecurityProperties();
        filter = new OpenApiSignFilter(secretProvider, properties, redisTemplate);
    }

    @AfterEach
    void tearDown() {
        AuthenticationSupport.clear();
    }

    private AppIdentity app() {
        AppIdentity app = new AppIdentity();
        app.setAccountId(100L);
        app.setAppKey(APP_KEY);
        app.setAppSecret(APP_SECRET);
        app.setAppName("合作方A");
        app.setScopes(Collections.singleton("order:read"));
        return app;
    }

    private MockHttpServletRequest signedRequest(String body, long timestamp, String nonce) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openapi/order/list");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body == null ? new byte[0] : body.getBytes());
        request.addHeader(OpenApiSignFilter.HEADER_APP_KEY, APP_KEY);
        request.addHeader(OpenApiSignFilter.HEADER_TIMESTAMP, String.valueOf(timestamp));
        request.addHeader(OpenApiSignFilter.HEADER_NONCE, nonce);
        String signBase = APP_KEY + "\n" + timestamp + "\n" + nonce + "\n"
                + com.scaffold.starter.security.openapi.SignUtils.sha256Hex(body == null ? "" : body);
        request.addHeader(OpenApiSignFilter.HEADER_SIGN,
                com.scaffold.starter.security.openapi.SignUtils.hmacSha256Hex(signBase, APP_SECRET));
        return request;
    }


    @Test
    void missingHeadersShouldBeRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openapi/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertEquals(401, doFilter(request, response));
    }

    @Test
    void unknownAppKeyShouldBeRejected() throws Exception {
        MockHttpServletRequest request = signedRequest("{}", NOW, "n1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(secretProvider.load(APP_KEY)).thenReturn(null);
        assertEquals(401, doFilter(request, response));
    }

    @Test
    void staleTimestampShouldBeRejected() throws Exception {
        MockHttpServletRequest request = signedRequest("{}", NOW - 10 * 60 * 1000, "n2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(secretProvider.load(APP_KEY)).thenReturn(app());
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);
        assertEquals(401, doFilter(request, response));
    }

    @Test
    void replayedNonceShouldBeRejectedWithConflict() throws Exception {
        MockHttpServletRequest request = signedRequest("{\"a\":1}", NOW, "dup");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(secretProvider.load(APP_KEY)).thenReturn(app());
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(false);
        assertEquals(409, doFilter(request, response));
    }

    @Test
    void wrongSignatureShouldBeRejected() throws Exception {
        MockHttpServletRequest request = signedRequest("{\"a\":1}", NOW, "n3");
        request.removeHeader(OpenApiSignFilter.HEADER_SIGN);
        request.addHeader(OpenApiSignFilter.HEADER_SIGN, "deadbeef");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(secretProvider.load(APP_KEY)).thenReturn(app());
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);
        assertEquals(401, doFilter(request, response));
    }

    @Test
    void validSignatureShouldPassAndApplyAppIdentity() throws Exception {
        MockHttpServletRequest request = signedRequest("{\"a\":1}", NOW, "n4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(secretProvider.load(APP_KEY)).thenReturn(app());
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);

        final String[] accountNoInChain = {null};
        javax.servlet.FilterChain chain = new javax.servlet.FilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
                accountNoInChain[0] = LoginContext.currentOrNull() == null
                        ? null : LoginContext.current().getAccountNo();
            }
        };

        filter.doFilter(request, response, chain);

        assertEquals(APP_KEY, accountNoInChain[0], "合法签名应在链内装配 APP 身份");
        assertNull(LoginContext.currentOrNull(), "请求结束后上下文必须清理");
    }

    private int doFilter(MockHttpServletRequest request, MockHttpServletResponse response)
            throws Exception {
        javax.servlet.FilterChain chain = mock(javax.servlet.FilterChain.class);
        filter.doFilter(request, response, chain);
        assertTrue(response.getContentAsString().contains("\"code\":"));
        return response.getStatus();
    }
}
