package com.scaffold.starter.push;

import com.scaffold.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;

/**
 * 基于 Redis pub/sub 的多实例转发实现：发布到统一频道，各节点订阅后本机投递。
 *
 * <p>pub/sub 是「尽力而为」语义（节点短瞬掉线会错过消息）——与 SSE 本身
 * 的连接语义匹配；需要必达的推送（如支付结果）请以落库轮询/补偿为准，
 * 推送只做「实时加速」。</p>
 *
 * @author lianghang
 */
public class RedisPushRelay implements PushRelay {

    private static final Logger log = LoggerFactory.getLogger(RedisPushRelay.class);

    private final StringRedisTemplate redisTemplate;
    private final String topic;

    public RedisPushRelay(StringRedisTemplate redisTemplate, String topic) {
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    @Override
    public void relay(PushMessage message) {
        String json = JsonUtils.toJson(message);
        if (json == null) {
            return;
        }
        redisTemplate.convertAndSend(topic, json);
        if (log.isDebugEnabled()) {
            log.debug("[push] 已扇出 event={} accountId={}", message.getEvent(), message.getAccountId());
        }
    }

    /** 订阅回调：交给 {@link PushService#onRelayedMessage} 本机投递。 */
    MessageListener listener(final PushService pushService) {
        return new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    pushService.onRelayedMessage(new String(message.getBody(), StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.warn("[push] 转发消息处理失败", e);
                }
            }
        };
    }
}
