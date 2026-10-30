package com.scaffold.starter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 缓存自动装配器，提供默认的两级缓存实现。
 *
 * <p>默认装配 Caffeine + Redis 的 {@code TwoLevelCacheManager}，仅在容器中尚无
 * {@code CacheManager} 时生效，业务方可自行注册实现覆盖；配置前缀为
 * {@code scaffold.cache}，同时启用声明式缓存注解支持。</p>
 *
 * <p>当 {@code scaffold.cache.type=redis} 时不注册任何 Bean，退化为 Boot
 * 自动配置提供的纯 Redis 缓存——两级缓存适合字典、配置类读多写少的数据，
 * 强一致场景请勿依赖本装配器。</p>
 *
 * @author lianghang
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CachePropertiesHolder.class)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager twoLevelCacheManager(StringRedisTemplate redisTemplate,
                                             ObjectMapper objectMapper,
                                             CachePropertiesHolder properties) {
        return new TwoLevelCacheManager(
                Caffeine.newBuilder()
                        .maximumSize(properties.getL1MaxSize())
                        .expireAfterWrite(Duration.ofSeconds(properties.getL1TtlSeconds()))
                        .build(),
                redisTemplate, objectMapper,
                properties.getKeyPrefix(),
                Duration.ofSeconds(properties.getL2TtlSeconds()));
    }
}
