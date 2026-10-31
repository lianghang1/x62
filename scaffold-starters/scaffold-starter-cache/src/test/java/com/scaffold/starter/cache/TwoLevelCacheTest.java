package com.scaffold.starter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 两级缓存测试：L1 命中零 Redis、L2 命中回填 L1、回源落双级、失效、
 * 序列化故障降级、坏 JSON 容错、管理器按名去重。
 *
 * @author lianghang
 */
class TwoLevelCacheTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TwoLevelCacheManager manager;
    private static final String PREFIX = "scaffold:cache:";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        manager = new TwoLevelCacheManager(Caffeine.newBuilder().build(), redisTemplate,
                objectMapper, "scaffold:cache:", Duration.ofSeconds(60));
    }

    @Test
    void l1HitShouldNotTouchRedis() {
        Cache cache = manager.getCache("dict");
        cache.put("k1", "v1");
        org.mockito.Mockito.clearInvocations(redisTemplate, valueOps);

        assertEquals("v1", cache.get("k1", String.class));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void l2HitShouldBackfillL1() throws Exception {
        Cache cache = manager.getCache("dict");
        when(valueOps.get(PREFIX + "dict:k2"))
                .thenReturn(objectMapper.writeValueAsString("v2"));

        assertEquals("v2", cache.get("k2", String.class));

        // 回填后再次读取不再访问 Redis
        org.mockito.Mockito.clearInvocations(redisTemplate, valueOps);
        assertEquals("v2", cache.get("k2", String.class));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void loaderShouldBeInvokedOnceAndCached() {
        Cache cache = manager.getCache("dict");
        AtomicInteger loads = new AtomicInteger();
        when(valueOps.get(anyString())).thenReturn(null);

        String v = cache.get("k3", () -> {
            loads.incrementAndGet();
            return "loaded";
        });
        assertEquals("loaded", v);
        assertEquals(1, loads.get());

        cache.get("k3", () -> {
            loads.incrementAndGet();
            return "loaded";
        });
        assertEquals(1, loads.get(), "第二次应命中缓存不再回源");
        verify(valueOps).set(contains("dict:k3"), eq("\"loaded\""), eq(Duration.ofSeconds(60)));
    }

    @Test
    void evictShouldDeleteL2AndInvalidateL1() {
        Cache cache = manager.getCache("dict");
        cache.put("k4", "v4");
        cache.evict("k4");

        verify(redisTemplate).delete(PREFIX + "dict:k4");
        when(valueOps.get(PREFIX + "dict:k4")).thenReturn(null);
        assertNull(cache.get("k4", String.class));
    }

    @Test
    void redisSetFailureShouldNotBreakBusiness() {
        Cache cache = manager.getCache("dict");
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(valueOps).set(anyString(), anyString(), eq(Duration.ofSeconds(60)));

        // 写 L2 失败按未命中处理：不向业务抛异常（本次也未落 L1，与实现语义一致）
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> cache.put("k5", "v5"));
        assertNull(cache.get("k5", String.class));
    }

    @Test
    void brokenJsonInL2ShouldBeTreatedAsMiss() {
        Cache cache = manager.getCache("dict");
        when(valueOps.get(PREFIX + "dict:k6")).thenReturn("{not-json");

        assertNull(cache.get("k6", String.class));
    }

    @Test
    void loaderFailureShouldWrapAsValueRetrievalException() {
        Cache cache = manager.getCache("dict");
        when(valueOps.get(anyString())).thenReturn(null);

        assertThrows(Cache.ValueRetrievalException.class,
                () -> cache.get("k7", () -> {
                    throw new IllegalStateException("boom");
                }));
    }

    @Test
    void managerShouldDeduplicateByName() {
        Cache a1 = manager.getCache("dict");
        Cache a2 = manager.getCache("dict");
        Cache b = manager.getCache("other");
        assertSame(a1, a2);
        assertNotNull(b);
    }
}
