package com.scaffold.starter.push.sse;

import com.scaffold.starter.push.PushChannel;
import com.scaffold.starter.push.PushMessage;

/**
 * SSE 推送通道：把 {@link PushMessage} 路由到本机 SSE 连接注册表。
 *
 * @author lianghang
 */
public class SsePushChannel implements PushChannel {

    private final SseEmitterRegistry registry;

    public SsePushChannel(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void push(PushMessage message) {
        if (message.getAccountId() == null) {
            registry.broadcast(message.getEventId(), message.getEvent(), message.getPayload());
        } else {
            registry.send(message.getAccountId(), message.getEventId(), message.getEvent(),
                    message.getPayload());
        }
    }
}
