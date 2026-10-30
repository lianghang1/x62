package com.scaffold.starter.push.sse;

import com.scaffold.core.util.JsonUtils;
import com.scaffold.starter.push.PushProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * SSE 连接注册表：按 accountId 管理本机全部 SSE 连接。
 *
 * <p>生命周期由 Spring 回调托管——完成/超时/异常时自动摘除，业务无需手动清理；
 * 发送失败视为死连接立即摘除，绝不让单个死连接阻断同账户其他连接的投递。
 * 心跳（{@code hb} 事件）由装配层的定时器调用 {@link #heartbeat()} 维持，
 * 防止代理/网关掐断空闲长连接。</p>
 *
 * @author lianghang
 */
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    /** 心跳事件名（与 linkora 服务端约定一致，客户端识别后不落业务） */
    public static final String HEARTBEAT_EVENT = "hb";

    private final Map<Long, List<SseConnection>> connections = new ConcurrentHashMap<Long, List<SseConnection>>();

    private final PushProperties properties;

    public SseEmitterRegistry(PushProperties properties) {
        this.properties = properties;
    }

    /**
     * 为账户新建 SSE 连接并登记。
     *
     * <p>emitter 超时按配置（默认 0 = 永不超时，存活靠心跳）；业务侧
     * 只需把返回值写给响应即可，断开清理是自动的。</p>
     */
    public SseEmitter register(Long accountId) {
        SseEmitter emitter = new SseEmitter(properties.getSseTimeoutMs());
        SseConnection connection = new SseConnection(accountId, emitter);
        connections.computeIfAbsent(accountId, k -> new CopyOnWriteList()).add(connection);
        Runnable remove = () -> remove(accountId, connection);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove(accountId, connection));
        log.info("[push] SSE 连接建立 accountId={} connection={} 在线连接数={}",
                accountId, connection.getId(), localCount());
        return emitter;
    }

    /** 向账户的全部 SSE 连接投递事件；单个连接失败仅摘除自身。 */
    public int send(Long accountId, String eventId, String event, Object payload) {
        return sendTo(accountId, eventId, event, payload);
    }

    /** 向本机全部 SSE 连接广播。 */
    public int broadcast(String eventId, String event, Object payload) {
        int sent = 0;
        for (Long accountId : connections.keySet()) {
            sent += sendTo(accountId, eventId, event, payload);
        }
        return sent;
    }

    /** 全量连接发心跳（{@code hb} 事件），返回存活连接数。 */
    public int heartbeat() {
        return broadcast(null, HEARTBEAT_EVENT, "ping");
    }

    /** 本机 SSE 在线连接总数（多实例场景仅本节点视角）。 */
    public int localCount() {
        int count = 0;
        for (List<SseConnection> list : connections.values()) {
            count += list.size();
        }
        return count;
    }

    /** 某账户在本机的连接数。 */
    public int localCount(Long accountId) {
        List<SseConnection> list = connections.get(accountId);
        return list == null ? 0 : list.size();
    }

    private int sendTo(Long accountId, String eventId, String event, Object payload) {
        List<SseConnection> list = connections.get(accountId);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (SseConnection connection : new ArrayList<SseConnection>(list)) {
            try {
                SseEmitter.SseEventBuilder builder = SseEmitter.event()
                        .name(event)
                        .data(JsonUtils.toJson(payload), MediaType.APPLICATION_JSON);
                if (eventId != null) {
                    builder.id(eventId);
                }
                connection.getEmitter().send(builder);
                sent++;
            } catch (Exception e) {
                log.warn("[push] SSE 投递失败，摘除连接 accountId={} connection={}: {}",
                        accountId, connection.getId(), e.getMessage());
                remove(accountId, connection);
            }
        }
        return sent;
    }

    private void remove(Long accountId, SseConnection connection) {
        List<SseConnection> list = connections.get(accountId);
        if (list != null) {
            list.remove(connection);
            if (list.isEmpty()) {
                connections.remove(accountId, list);
            }
        }
    }

    /** 回调线程与业务线程并发摘除的线程安全列表（JDK8 无 List.of）。 */
    private static final class CopyOnWriteList extends java.util.concurrent.CopyOnWriteArrayList<SseConnection> {
    }
}
