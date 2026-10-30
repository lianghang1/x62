package com.scaffold.starter.protection;

import com.scaffold.starter.protection.abuse.AbuseException;
import com.scaffold.starter.protection.abuse.AbuseGuard;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AbuseGuard 防刷闸门测试（mock Redis 验证三种闸门的判定语义）。
 *
 * @author lianghang
 */
class AbuseGuardTest {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    private AbuseGuard newGuard() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        return new AbuseGuard(redisTemplate);
    }

    @Test
    void intervalGateRejectsSecondCallWithinWindow() {
        AbuseGuard guard = newGuard();
        // 第一次 setIfAbsent 成功，第二次已存在 → 拒绝
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true, false);

        assertDoesNotThrow(() -> guard.checkInterval("sms", "13800000000", 60));
        AbuseException exception = assertThrows(AbuseException.class,
                () -> guard.checkInterval("sms", "13800000000", 60));
        assertEquals("sms", exception.getScene());
    }

    @Test
    void windowGateAllowsWithinLimitAndRejectsBeyond() {
        AbuseGuard guard = newGuard();
        // 计数序列：1..5 放行，6 超限
        when(valueOps.increment(anyString())).thenReturn(1L, 2L, 3L, 4L, 5L, 6L);

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> guard.checkWindow("sms", "global", 5, Duration.ofHours(1)));
        }
        assertThrows(AbuseException.class,
                () -> guard.checkWindow("sms", "global", 5, Duration.ofHours(1)));
    }

    @Test
    void dailyGateRejectsBeyondQuota() {
        AbuseGuard guard = newGuard();
        // 天闸 key 带日期：超限抛 429
        when(valueOps.increment(anyString())).thenReturn(11L);

        assertThrows(AbuseException.class, () -> guard.checkDaily("sms", "ip:1.2.3.4", 10));
    }

    @Test
    void hitCountsAndCurrentReads() {
        AbuseGuard guard = newGuard();
        when(valueOps.increment(anyString())).thenReturn(3L);
        when(valueOps.get(anyString())).thenReturn("3");

        assertEquals(3L, guard.hit("login", "1.2.3.4", Duration.ofMinutes(10)));
        assertEquals(3L, guard.current("login", "1.2.3.4"));

        Mockito.verify(redisTemplate, Mockito.never()).delete(anyString());
    }
}
