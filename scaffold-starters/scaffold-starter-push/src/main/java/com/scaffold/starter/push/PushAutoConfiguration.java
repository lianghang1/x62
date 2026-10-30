package com.scaffold.starter.push;

import com.scaffold.starter.push.sse.OnlineSubscribeController;
import com.scaffold.starter.push.sse.SseEmitterRegistry;
import com.scaffold.starter.push.sse.SsePushChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 推送自动装配入口：SSE 注册表 + 通道 + 可选 Redis 多实例转发 + 心跳定时器。
 *
 * <p>单实例（默认）：PushService 直接本机投递；多实例
 * （{@code scaffold.push.multi-instance=true}）：Redis pub/sub 扇出。
 * 心跳定时器随容器关闭而停止。</p>
 *
 * @author lianghang
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "scaffold.push", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PushProperties.class)
public class PushAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PushAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public SseEmitterRegistry sseEmitterRegistry(PushProperties properties) {
        return new SseEmitterRegistry(properties);
    }

    @Bean
    @ConditionalOnMissingBean(PushRelay.class)
    public PushRelay pushRelay(PushProperties properties, StringRedisTemplate redisTemplate,
                               RedisConnectionFactory connectionFactory,
                               ObjectProvider<PushService> pushServiceProvider) {
        if (!properties.isMultiInstance()) {
            // 单实例：空转发，消息不走 Redis 直接本机投递。
            // PushService 与本转发器互为依赖，这里经 ObjectProvider 延迟解析，
            // 推送发生时容器已就绪，避免 BeanCurrentlyInCreationException
            return message -> pushServiceProvider.getObject().dispatchLocal(message);
        }
        RedisPushRelay relay = new RedisPushRelay(redisTemplate, properties.getRelayTopic());
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(relay.listener(pushServiceProvider.getObject()),
                new ChannelTopic(properties.getRelayTopic()));
        log.info("[push] 已启用 Redis 多实例转发 topic={}", properties.getRelayTopic());
        return relay;
    }

    @Bean
    @ConditionalOnMissingBean
    public PushService pushService(PushRelay relay, SseEmitterRegistry registry) {
        PushService service = new PushService(relay);
        service.registerChannel(new SsePushChannel(registry));
        return service;
    }

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService pushHeartbeatScheduler(SseEmitterRegistry registry,
                                                           PushProperties properties) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "push-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        if (properties.getHeartbeatSeconds() > 0) {
            scheduler.scheduleAtFixedRate(registry::heartbeat,
                    properties.getHeartbeatSeconds(), properties.getHeartbeatSeconds(), TimeUnit.SECONDS);
            log.info("[push] SSE 心跳已启动 interval={}s", properties.getHeartbeatSeconds());
        }
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean
    public OnlineSubscribeController onlineSubscribeController(SseEmitterRegistry registry) {
        return new OnlineSubscribeController(registry);
    }
}
