package com.scaffold.starter.protection;

import com.scaffold.common.exception.BizException;
import com.scaffold.starter.protection.ratelimit.RateLimit;
import com.scaffold.starter.protection.ratelimit.RateLimitAspect;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 限流切面测试：Lua 计数放行/超限、维度 key 组装、无请求上下文放行。
 *
 * @author lianghang
 */
class RateLimitAspectTest {

    private StringRedisTemplate redisTemplate;
    private RateLimitAspect aspect;
    private JoinPoint joinPoint;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        aspect = new RateLimitAspect(redisTemplate);
        joinPoint = mock(JoinPoint.class);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindRequest(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private RateLimit rateLimit(int limit, int window, String dimension, String key) {
        return new RateLimit() {
            @Override
            public int limit() {
                return limit;
            }

            @Override
            public int windowSeconds() {
                return window;
            }

            @Override
            public String dimension() {
                return dimension;
            }

            @Override
            public String key() {
                return key;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }
        };
    }

    @Test
    void belowLimitShouldPass() {
        bindRequest(new MockHttpServletRequest("GET", "/api/goods"));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

        assertDoesNotThrow(() -> aspect.check(joinPoint, rateLimit(10, 1, "uri", "")));
    }

    @Test
    void overLimitShouldThrow429() {
        bindRequest(new MockHttpServletRequest("GET", "/api/goods"));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(11L);

        BizException e = assertThrows(BizException.class,
                () -> aspect.check(joinPoint, rateLimit(10, 1, "uri", "")));
        assertEquals(1429, e.getErrorCode().getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void uriDimensionShouldBuildKeyFromMethodAndUri() {
        bindRequest(new MockHttpServletRequest("GET", "/api/goods"));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

        aspect.check(joinPoint, rateLimit(10, 5, "uri", ""));

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass((Class) List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(), anyString());
        assertEquals("scaffold:ratelimit:uri:GET:/api/goods", keys.getValue().get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void customKeyShouldBePartOfLimitKey() {
        bindRequest(new MockHttpServletRequest("GET", "/api/x"));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

        aspect.check(joinPoint, rateLimit(10, 5, "uri", "export"));

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass((Class) List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(), anyString());
        assertTrue(keys.getValue().get(0).contains("uri:export:"));
    }

    @Test
    void accountDimensionShouldPreferHeaderOverIp() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/x");
        request.addHeader("X-Account-Id", "42");
        bindRequest(request);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

        assertDoesNotThrow(() -> aspect.check(joinPoint, rateLimit(10, 1, "account", "")));
        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass((Class) List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(), anyString());
        assertTrue(keys.getValue().get(0).endsWith(":42"));
    }

    @Test
    void noRequestContextShouldPassWithoutRedisCall() {
        // 模拟内部调用：无请求上下文
        assertDoesNotThrow(() -> aspect.check(joinPoint, rateLimit(10, 1, "uri", "")));
        verifyNoInteractions(redisTemplate);
    }
}
