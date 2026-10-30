package com.scaffold.starter.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 两级缓存管理器，按缓存名懒创建并复用 {@code TwoLevelCache} 实例。
 *
 * <p>所有缓存共享同一套 L1（Caffeine）、Redis 客户端与 L2 TTL，仅以缓存名区分
 * key 空间；实例按名去重缓存于 {@code ConcurrentHashMap}，避免重复构建。</p>
 *
 * @author lianghang
 */
public class TwoLevelCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    private final com.github.benmanes.caffeine.cache.Cache<String, Object> l1;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final String keyPrefix;
    private final java.time.Duration l2Ttl;

    public TwoLevelCacheManager(com.github.benmanes.caffeine.cache.Cache<String, Object> l1,
                                org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                                com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                String keyPrefix, java.time.Duration l2Ttl) {
        this.l1 = l1;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix;
        this.l2Ttl = l2Ttl;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name,
                n -> new TwoLevelCache(n, l1, redisTemplate, objectMapper, keyPrefix, l2Ttl, true));
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }
}
