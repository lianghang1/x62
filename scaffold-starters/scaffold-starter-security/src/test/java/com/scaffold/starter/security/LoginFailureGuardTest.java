package com.scaffold.starter.security;

import com.scaffold.starter.security.config.SecurityProperties;
import com.scaffold.starter.security.policy.LoginFailureGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录失败锁定测试：账号计数/锁定阈值/成功清零、IP 防撞库开关与窗口。
 *
 * @author lianghang
 */
class LoginFailureGuardTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private SecurityProperties.PasswordPolicy policy;
    private SecurityProperties.IpGuard ipGuard;
    private LoginFailureGuard guard;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        policy = new SecurityProperties.PasswordPolicy();
        ipGuard = new SecurityProperties.IpGuard();
        guard = new LoginFailureGuard(redisTemplate, policy, ipGuard);
    }

    @Test
    void lockedWhenCountReachesMax() {
        when(valueOps.get(anyString())).thenReturn("4");
        assertFalse(guard.isLocked("alice"));
        when(valueOps.get(anyString())).thenReturn("5");
        assertTrue(guard.isLocked("alice"));
    }

    @Test
    void recordFailureShouldExpireOnlyOnFirstCount() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        assertEquals(1, guard.recordFailure("alice"));
        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(policy.getLockSeconds())));

        when(valueOps.increment(anyString())).thenReturn(2L);
        assertEquals(2, guard.recordFailure("alice"));
        // 第二次失败不再续 TTL
        verify(redisTemplate, never()).expire(anyString(), eq(Duration.ZERO));
    }

    @Test
    void resetShouldDeleteKey() {
        guard.reset("alice");
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void ipGuardDisabledShouldNeverBlock() {
        ipGuard.setEnabled(false);
        assertFalse(guard.isIpBlocked("1.2.3.4"));
        assertEquals(0, guard.recordIpFailure("1.2.3.4"));
    }

    @Test
    void ipGuardEnabledShouldBlockAtLimit() {
        ipGuard.setEnabled(true);
        when(valueOps.get(anyString())).thenReturn(String.valueOf(ipGuard.getFailLimit()));
        assertTrue(guard.isIpBlocked("1.2.3.4"));

        when(valueOps.increment(anyString())).thenReturn(1L);
        assertEquals(1, guard.recordIpFailure("1.2.3.4"));
        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(ipGuard.getWindowSeconds())));
    }
}
