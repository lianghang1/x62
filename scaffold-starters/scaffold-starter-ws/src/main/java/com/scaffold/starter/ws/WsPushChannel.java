package com.scaffold.starter.ws;

import com.scaffold.starter.push.PushChannel;
import com.scaffold.starter.push.PushMessage;
import com.scaffold.common.util.JsonUtils;

/**
 * WebSocket 推送通道：挂入 {@link com.scaffold.starter.push.PushService}
 * 的通道列表，与 SSE 通道并列——业务一次推送，两通道同时到达。
 *
 * @author lianghang
 */
public class WsPushChannel implements PushChannel {

    private final WsSessionRegistry registry;

    public WsPushChannel(WsSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void push(PushMessage message) {
        String text = JsonUtils.toJson(message);
        if (message.getAccountId() == null) {
            registry.broadcast(text);
        } else {
            registry.send(message.getAccountId(), text);
        }
    }
}
