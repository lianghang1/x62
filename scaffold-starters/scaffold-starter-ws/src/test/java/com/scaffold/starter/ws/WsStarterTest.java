package com.scaffold.starter.ws;

import com.scaffold.starter.security.token.SessionInfo;
import com.scaffold.starter.security.token.TokenService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WebSocket 层行为测试：会话注册表与握手拦截器。
 *
 * @author lianghang
 */
class WsStarterTest {

    @Test
    void registrySendBroadcastAndCount() {
        WsSessionRegistry registry = new WsSessionRegistry();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        registry.register(1L, session);
        assertEquals(1, registry.localCount());
        assertEquals(1, registry.send(1L, "hello"));
        assertEquals(1, registry.broadcast("hi"));

        registry.unregister(1L, session);
        assertEquals(0, registry.localCount());
        assertEquals(0, registry.send(1L, "x"));
    }

    @Test
    void closedOrBrokenSessionIsRemovedOnSend() {
        WsSessionRegistry registry = new WsSessionRegistry();
        WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.isOpen()).thenReturn(false);
        registry.register(2L, closed);

        assertEquals(0, registry.send(2L, "x"));
        assertEquals(0, registry.localCount());
    }

    @Test
    void handshakeAcceptsValidTokenFromQuery() {
        TokenService tokenService = mock(TokenService.class);
        SessionInfo info = new SessionInfo();
        info.setAccountId(9L);
        when(tokenService.resolve("good-token")).thenReturn(info);

        WsHandshakeInterceptor interceptor = new WsHandshakeInterceptor(tokenService);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setQueryString("token=good-token");
        org.springframework.http.server.ServletServerHttpRequest request =
                new org.springframework.http.server.ServletServerHttpRequest(servletRequest);
        org.springframework.http.server.ServerHttpResponse mockResponse =
                Mockito.mock(org.springframework.http.server.ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<String, Object>();

        assertTrue(interceptor.beforeHandshake(request, mockResponse, null, attributes));
        assertEquals(9L, attributes.get(ScaffoldWebSocketHandler.ATTR_ACCOUNT_ID));
    }

    @Test
    void handshakeRejectsInvalidToken() {
        TokenService tokenService = Mockito.mock(TokenService.class);
        when(tokenService.resolve(anyString())).thenReturn(null);

        WsHandshakeInterceptor interceptor = new WsHandshakeInterceptor(tokenService);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setQueryString("token=bad");
        org.springframework.http.server.ServletServerHttpRequest request =
                new org.springframework.http.server.ServletServerHttpRequest(servletRequest);

        assertFalse(interceptor.beforeHandshake(request,
                Mockito.mock(org.springframework.http.server.ServerHttpResponse.class), null,
                new HashMap<String, Object>()));
    }

    @Test
    void wsPushChannelSendsJsonToRegistry() {
        WsSessionRegistry registry = Mockito.spy(WsSessionRegistry.class);
        // 仅验证通道把消息序列化为 JSON 文本并委托 registry（广播语义）
        WsPushChannel channel = new WsPushChannel(registry);
        try {
            channel.push(com.scaffold.starter.push.PushMessage.of("event-only", "payload"));
        } catch (Exception ignored) {
            // Mockito spy 对 final 类方法的限制不影响行为断言
        }
        Mockito.verify(registry).broadcast(Mockito.contains("event-only"));
    }
}
