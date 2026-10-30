package com.scaffold.starter.push;

import com.scaffold.core.util.JsonUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 推送门面：业务侧唯一入口，负责通道扇出与跨实例转发。
 *
 * <p>单实例：消息直接在本机各通道（SSE/WebSocket）投递；多实例
 * （{@code scaffold.push.multi-instance=true}）：经 {@link PushRelay}
 * 扇出到全部节点，各节点订阅回调后本机投递——同一账户的连接散布在
 * 多个节点上时依然可达。</p>
 *
 * @author lianghang
 */
public class PushService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PushService.class);

    private final List<PushChannel> channels = new CopyOnWriteArrayList<PushChannel>();
    private final PushRelay relay;

    public PushService(PushRelay relay) {
        this.relay = relay;
    }

    /** 注册通道（SSE/WebSocket 装配时自动加入）。 */
    public void registerChannel(PushChannel channel) {
        channels.add(channel);
    }

    /** 推送给指定账户的全部在线连接（所有通道）。 */
    public void pushToUser(Long accountId, String event, Object payload) {
        dispatch(PushMessage.of(accountId, event, payload));
    }

    /** 广播给本应用全部在线连接（所有通道）。 */
    public void broadcast(String event, Object payload) {
        dispatch(PushMessage.of(event, payload));
    }

    /** 统一分发：有 relay 走扇出（各节点本机投递），否则直接本机投递。 */
    void dispatch(PushMessage message) {
        if (relay != null) {
            relay.relay(message);
        } else {
            dispatchLocal(message);
        }
    }

    /** 本机投递：逐通道扇出，单通道异常不影响其他通道。 */
    void dispatchLocal(PushMessage message) {
        for (PushChannel channel : channels) {
            try {
                channel.push(message);
            } catch (Exception e) {
                log.warn("[push] 通道投递失败 event={} accountId={}",
                        message.getEvent(), message.getAccountId(), e);
            }
        }
    }

    /** 供 Relay 监听器回调：收到跨实例消息后本机投递。 */
    public void onRelayedMessage(String json) {
        PushMessage message = JsonUtils.fromJson(json, PushMessage.class);
        if (message != null) {
            dispatchLocal(message);
        }
    }

    /** 消息转 JSON（relay 序列化统一走 JsonUtils）。 */
    String toJson(PushMessage message) {
        return JsonUtils.toJson(message);
    }
}
