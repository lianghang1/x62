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
 * 缓存自动装配：默认两级缓存；scaffold.cache.type=redis 时退化为纯 Redis 缓存（由 Boot 自动配置提供）。
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
