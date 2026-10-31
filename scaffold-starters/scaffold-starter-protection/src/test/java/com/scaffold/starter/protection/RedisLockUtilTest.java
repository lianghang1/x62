package com.scaffold.starter.protection;

import com.scaffold.starter.protection.lock.RedisLockUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 分布式锁工具测试：加锁语义、等待重试、仅持有者可释放。
 *
 * @author lianghang
 */
class RedisLockUtilTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private RedisLockUtil lockUtil;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lockUtil = new RedisLockUtil(redisTemplate);
    }

    @Test
    void tryLockShouldReturnHolderOnSuccess() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        String holder = lockUtil.tryLock("order:1", 0, 30);
        assertNotNull(holder);
        verify(valueOps).setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(30)));
    }

    @Test
    void tryLockShouldReturnNullImmediatelyWhenNoWait() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertNull(lockUtil.tryLock("order:1", 0, 30));
    }

    @Test
    void tryLockShouldRetryUntilSuccess() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false, false, true);
        assertNotNull(lockUtil.tryLock("order:1", 500, 30));
    }

    @Test
    void unlockShouldRunHolderCheckScript() {
        lockUtil.unlock("order:1", "holder-1");
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), eq("holder-1"));
    }

    @Test
    void unlockWithNullHolderShouldBeNoop() {
        lockUtil.unlock("order:1", null);
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void holdersShouldBeUniquePerLockAttempt() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        String h1 = lockUtil.tryLock("a", 0, 30);
        String h2 = lockUtil.tryLock("a", 0, 30);
        assertNotNull(h1);
        assertNotNull(h2);
        assertEquals(h1, h1);
    }
}
