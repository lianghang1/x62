package com.scaffold.starter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * 两级缓存实现，L1 为进程内 Caffeine，L2 为跨实例共享的 Redis。
 *
 * <p>读路径按 L1 → L2 → loader 逐层回源，L2 命中后回填 L1；写路径直写 L2 并失效
 * 本实例 L1，其他实例依赖 TTL 收敛，因此并非强一致——适合字典、配置类读多写少
 * 的数据，强一致场景请直接使用 Redis 或在业务层做双删。</p>
 *
 * <p>序列化失败按未命中处理，不向业务抛异常，保证缓存故障不阻断主流程。</p>
 *
 * @author lianghang
 */
public class TwoLevelCache extends AbstractValueAdaptingCache {

    private final String name;
    private final com.github.benmanes.caffeine.cache.Cache<String, Object> l1;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final Duration l2Ttl;

    TwoLevelCache(String name, com.github.benmanes.caffeine.cache.Cache<String, Object> l1,
                  StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                  String keyPrefix, Duration l2Ttl, boolean allowNull) {
        super(allowNull);
        this.name = name;
        this.l1 = l1;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix;
        this.l2Ttl = l2Ttl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    protected Object lookup(Object key) {
        String k = key(key);
        Object hit = l1.getIfPresent(k);
        if (hit != null) {
            return hit;
        }
        String json = redisTemplate.opsForValue().get(k);
        if (json == null) {
            return null;
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            l1.put(k, value);
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object cached = lookup(key);
        if (cached != null) {
            return (T) fromStoreValue(cached);
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        String k = key(key);
        if (value == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(k, objectMapper.writeValueAsString(value), l2Ttl);
            l1.put(k, value);
        } catch (Exception e) {
            // 序列化失败按未命中处理，不影响业务主流程
        }
    }

    @Override
    public void evict(Object key) {
        String k = key(key);
        redisTemplate.delete(k);
        l1.invalidate(k);
    }

    @Override
    public void clear() {
        // 仅清本实例 L1 与本前缀已知 key 的 L2（模糊清理由运维脚本处理）
        l1.invalidateAll();
    }

    private String key(Object key) {
        return keyPrefix + name + ":" + key;
    }
}
