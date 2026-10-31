package com.scaffold.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.starter.security.token.RedisTokenService;
import com.scaffold.starter.security.token.SessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 会话服务测试：创建（含独占下线）、解析、自动续期阈值、吊销与在线会话清理。
 *
 * @author lianghang
 */
class RedisTokenServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private SetOperations<String, String> setOps;
    private RedisTokenService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        setOps = mock(SetOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        // ttl 100 秒、续期阈值 20%：剩余 < 20 秒才续
        service = new RedisTokenService(redisTemplate, objectMapper, 100, 20);
    }

    private SessionInfo session() {
        SessionInfo s = new SessionInfo();
        s.setAccountId(1L);
        s.setAccountNo("alice");
        s.setDevice("web");
        return s;
    }

    @Test
    void createShouldWriteTokenAndAccountIndex() {
        String token = service.create(session(), false);
        assertEquals(32, token.length());
        verify(valueOps).set(contains(token), anyString(), eq(Duration.ofSeconds(100)));
        verify(setOps).add(anyString(), eq(token));
    }

    @Test
    void exclusiveCreateShouldRevokeAllFirst() {
        when(setOps.members(anyString())).thenReturn(Collections.singleton("old-token"));
        service.create(session(), true);
        // 先删旧 token（集合删除）与索引 key，再写新会话
        verify(redisTemplate).delete(anyList());
        verify(redisTemplate).delete(contains("auth:account"));
    }

    @Test
    void resolveShouldReturnNullForEmptyOrUnknownToken() {
        assertNull(service.resolve(""));
        assertNull(service.resolve(null));
        when(valueOps.get(anyString())).thenReturn(null);
        assertNull(service.resolve("nope"));
    }

    @Test
    void resolveShouldTreatBrokenJsonAsInvalid() {
        when(valueOps.get(anyString())).thenReturn("{not-json");
        assertNull(service.resolve("tok"));
    }

    @Test
    void resolveWithRenewShouldExpireWhenBelowThreshold() throws Exception {
        when(valueOps.get(anyString())).thenReturn(objectMapper.writeValueAsString(session()));
        when(redisTemplate.getExpire(anyString())).thenReturn(10L);
        service.resolveWithRenew("tok");
        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(100)));
    }

    @Test
    void resolveWithRenewShouldNotExpireWhenAboveThreshold() throws Exception {
        when(valueOps.get(anyString())).thenReturn(objectMapper.writeValueAsString(session()));
        when(redisTemplate.getExpire(anyString())).thenReturn(80L);
        service.resolveWithRenew("tok");
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void revokeShouldDeleteTokenAndIndex() throws Exception {
        when(valueOps.get(anyString())).thenReturn(objectMapper.writeValueAsString(session()));
        service.revoke("tok");
        verify(redisTemplate).delete(contains("tok"));
        verify(setOps).remove(contains("auth:account"), eq("tok"));
    }

    @Test
    void onlineSessionsShouldDropStaleIndexEntries() throws Exception {
        Set<String> tokens = new HashSet<>();
        tokens.add("alive");
        tokens.add("stale");
        when(setOps.members(anyString())).thenReturn(tokens);
        String aliveJson = objectMapper.writeValueAsString(session());
        when(valueOps.get(contains("alive"))).thenReturn(aliveJson);
        when(valueOps.get(contains("stale"))).thenReturn(null);

        assertEquals(1, service.onlineSessions(1L).size());
        verify(setOps).remove(anyString(), eq("stale"));
    }
}
