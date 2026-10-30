package com.scaffold.starter.push;

import java.io.Serializable;
import java.util.UUID;

/**
 * 推送消息的信封：业务只填事件名与载荷，路由细节（本机/跨实例、通道选择）由推送层负责。
 *
 * <p>载荷按 {@code JsonUtils} 序列化后随事件下发；eventId 用于客户端
 * {@code Last-Event-ID} 续传与幂等去重，广播消息 accountId 为 null。</p>
 *
 * @author lianghang
 */
public class PushMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件唯一标识（SSE id 字段，客户端断线重连时以此续传） */
    private String eventId = UUID.randomUUID().toString().replace("-", "");

    /** 事件名（业务自定，如 {@code order.paid}；{@code hb} 保留为心跳） */
    private String event;

    /** 目标账户；null 表示广播 */
    private Long accountId;

    /** 业务载荷（任意可 JSON 序列化对象） */
    private Object payload;

    /** 发布时间戳（毫秒） */
    private long timestamp = System.currentTimeMillis();

    public static PushMessage of(String event, Object payload) {
        return of(null, event, payload);
    }

    public static PushMessage of(Long accountId, String event, Object payload) {
        PushMessage message = new PushMessage();
        message.event = event;
        message.accountId = accountId;
        message.payload = payload;
        return message;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
