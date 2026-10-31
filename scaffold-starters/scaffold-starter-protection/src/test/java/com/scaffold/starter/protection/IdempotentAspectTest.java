package com.scaffold.starter.protection;

import com.scaffold.common.exception.BizException;
import com.scaffold.starter.protection.idempotent.Idempotent;
import com.scaffold.starter.protection.idempotent.IdempotentAspect;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 幂等切面测试：窗口内重复提交拦截、幂等键组成、无请求上下文放行。
 *
 * @author lianghang
 */
class IdempotentAspectTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private IdempotentAspect aspect;
    private JoinPoint joinPoint;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        aspect = new IdempotentAspect(redisTemplate);
        joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"a1", null});
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindRequest(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private Idempotent idempotent(int windowSeconds) {
        return new Idempotent() {
            @Override
            public int windowSeconds() {
                return windowSeconds;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Idempotent.class;
            }
        };
    }

    @Test
    void firstCallShouldWin() {
        bindRequest(new MockHttpServletRequest("POST", "/api/order"));
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> aspect.check(joinPoint, idempotent(10)));
    }

    @Test
    void duplicateWithinWindowShouldBeRejected() {
        bindRequest(new MockHttpServletRequest("POST", "/api/order"));
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

        BizException e = assertThrows(BizException.class,
                () -> aspect.check(joinPoint, idempotent(10)));
        assertEquals(1409, e.getErrorCode().getCode());
    }

    @Test
    void idempotentKeyShouldCoverMethodUriIdentityAndArgs() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/order");
        request.addHeader("X-Account-Id", "42");
        bindRequest(request);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        aspect.check(joinPoint, idempotent(15));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(valueOps).setIfAbsent(key.capture(), anyString(),
                eq(Duration.ofSeconds(15)));
        assertTrue(key.getValue().contains("POST:/api/order:42:"),
                "幂等键应包含方法+URI+账户维度，实际=" + key.getValue());
    }

    @Test
    void noRequestContextShouldPassWithoutRedisCall() {
        assertDoesNotThrow(() -> aspect.check(joinPoint, idempotent(10)));
        verifyNoInteractions(redisTemplate);
    }
}
