package com.scaffold.starter.ws;

import com.scaffold.common.util.CollUtils;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 会话注册表：按 accountId 管理本机全部在线会话。
 *
 * <p>会话断开由 {@link ScaffoldWebSocketHandler} 回调摘除；
 * 与 SSE 注册表语义一致——本机视角，多实例经推送层转发，本类不做跨节点。</p>
 *
 * @author lianghang
 */
public class WsSessionRegistry {

    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> sessions =
            new ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>>();

    /** 登记会话（同一账户可有多端并发在线）。 */
    public void register(Long accountId, WebSocketSession session) {
        sessions.computeIfAbsent(accountId, k -> new CopyOnWriteArraySet<WebSocketSession>())
                .add(session);
    }

    /** 摘除会话（幂等）。 */
    public void unregister(Long accountId, WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> set = sessions.get(accountId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessions.remove(accountId, set);
            }
        }
    }

    /** 向账户全部会话发送文本；发送失败的会话直接关闭摘除。 */
    public int send(Long accountId, String text) {
        CopyOnWriteArraySet<WebSocketSession> set = sessions.get(accountId);
        if (CollUtils.isEmpty(set)) {
            return 0;
        }
        int sent = 0;
        for (WebSocketSession session : new ArrayList<WebSocketSession>(set)) {
            if (sendQuietly(session, text)) {
                sent++;
            } else {
                unregister(accountId, session);
            }
        }
        return sent;
    }

    /** 向本机全部会话广播。 */
    public int broadcast(String text) {
        int sent = 0;
        for (Long accountId : sessions.keySet()) {
            sent += send(accountId, text);
        }
        return sent;
    }

    /** 本机会话总数。 */
    public int localCount() {
        int count = 0;
        for (CopyOnWriteArraySet<WebSocketSession> set : sessions.values()) {
            count += set.size();
        }
        return count;
    }

    private boolean sendQuietly(WebSocketSession session, String text) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new org.springframework.web.socket.TextMessage(text));
                }
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
