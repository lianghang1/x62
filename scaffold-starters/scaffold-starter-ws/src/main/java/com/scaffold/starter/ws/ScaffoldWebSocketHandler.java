package com.scaffold.starter.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

/**
 * WebSocket 主处理器：连接登记、上行分发、断开摘除。
 *
 * <p> accountId 在握手阶段由 {@link WsHandshakeInterceptor} 写入会话属性，
 * 本处理器不再做鉴权；上行消息分发给全部 {@link WsMessageListener}，
 * 单监听器异常不影响其他监听器。</p>
 *
 * @author lianghang
 */
public class ScaffoldWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ScaffoldWebSocketHandler.class);

    /** 握手后写入会话属性的账户键 */
    public static final String ATTR_ACCOUNT_ID = "scaffold.accountId";

    private final WsSessionRegistry registry;
    private final List<WsMessageListener> listeners;

    public ScaffoldWebSocketHandler(WsSessionRegistry registry, List<WsMessageListener> listeners) {
        this.registry = registry;
        this.listeners = listeners == null ? java.util.Collections.<WsMessageListener>emptyList()
                : listeners;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long accountId = accountId(session);
        registry.register(accountId, session);
        log.info("[ws] 连接建立 accountId={} sessionId={} 在线会话={}",
                accountId, session.getId(), registry.localCount());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long accountId = accountId(session);
        for (WsMessageListener listener : listeners) {
            try {
                listener.onMessage(accountId, message.getPayload());
            } catch (Exception e) {
                log.warn("[ws] 上行消息处理失败 accountId={}", accountId, e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long accountId = accountId(session);
        registry.unregister(accountId, session);
        log.info("[ws] 连接关闭 accountId={} sessionId={} status={} 在线会话={}",
                accountId, session.getId(), status, registry.localCount());
    }

    private static Long accountId(WebSocketSession session) {
        Object value = session.getAttributes().get(ATTR_ACCOUNT_ID);
        return value instanceof Long ? (Long) value : 0L;
    }
}
