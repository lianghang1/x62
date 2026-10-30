package com.scaffold.module.system.biz.config;

import com.scaffold.common.constant.RedisKeys;
import com.scaffold.module.system.biz.service.ConfigServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 配置中心多实例同步装配：订阅配置失效广播频道。
 *
 * <p>类路径有 Redis 时注册监听容器，收到 {@link RedisKeys#CONFIG_REFRESH}
 * 广播后回源被改键；无 Redis（如 server-lite）时跳过——单实例下本机缓存
 * 已在保存时刷新，功能完整。</p>
 *
 * @author lianghang
 */
@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
public class ConfigCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(ConfigCacheConfig.class);

    @Bean
    public RedisMessageListenerContainer configRefreshListenerContainer(
            RedisConnectionFactory connectionFactory, ConfigServiceImpl configService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            String key = new String(message.getBody());
            try {
                configService.onRefreshMessage(key);
            } catch (Exception e) {
                log.warn("[config] 失效广播处理失败: key={}", key, e);
            }
        }, new ChannelTopic(RedisKeys.CONFIG_REFRESH));
        log.info("[config] 已订阅配置失效广播频道: {}", RedisKeys.CONFIG_REFRESH);
        return container;
    }
}
