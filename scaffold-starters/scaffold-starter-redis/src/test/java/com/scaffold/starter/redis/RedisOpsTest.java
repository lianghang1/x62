package com.scaffold.starter.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisOps 薄门面测试：TTL 强制、JSON 往返、一次性消费、计数、hash。
 *
 * @author lianghang
 */
class RedisOpsTest {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    private RedisOps redisOps;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
        redisOps = new RedisOps(redisTemplate);
    }

    /** 测试用的简单 POJO */
    public static class Demo {
        public String name;
        public int age;

        public Demo() {
        }

        public Demo(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    @Test
    void setRequiresExplicitTtl() {
        assertThrows(IllegalArgumentException.class, () -> redisOps.set("k", "v", null));
        assertThrows(IllegalArgumentException.class, () -> redisOps.set("k", "v", Duration.ofSeconds(-1)));
        // TTL 是必填语义：不存在无 TTL 的重载
        assertTrue(java.util.Arrays.stream(RedisOps.class.getDeclaredMethods())
                .noneMatch(m -> "set".equals(m.getName()) && m.getParameterCount() == 2));
    }

    @Test
    void setWithTtlWritesJson() {
        redisOps.set("k", new Demo("a", 1), Duration.ofMinutes(5));

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(eq("k"), value.capture(), eq(Duration.ofMinutes(5)));
        assertTrue(value.getValue().contains("\"name\":\"a\""));
    }

    @Test
    void getRoundtrip() {
        when(valueOps.get("k")).thenReturn("{\"name\":\"a\",\"age\":1}");
        Demo demo = redisOps.get("k", Demo.class);
        assertEquals("a", demo.name);
        assertEquals(1, demo.age);

        when(valueOps.get("miss")).thenReturn(null);
        assertNull(redisOps.get("miss", Demo.class));
    }

    @Test
    void getAndDeleteIsOneTime() {
        when(valueOps.get("otp")).thenReturn("{\"name\":\"a\",\"age\":1}");

        Demo demo = redisOps.getAndDelete("otp", Demo.class);
        assertEquals("a", demo.name);
        verify(redisTemplate).delete("otp");

        when(valueOps.get("otp")).thenReturn(null);
        assertNull(redisOps.getAndDelete("otp", Demo.class));
    }

    @Test
    void incrementAndExpire() {
        when(valueOps.increment("c", 1L)).thenReturn(3L);
        when(redisTemplate.expire("c", Duration.ofSeconds(60))).thenReturn(Boolean.TRUE);

        assertEquals(3, redisOps.increment("c", 1));
        assertTrue(redisOps.expire("c", Duration.ofSeconds(60)));
    }

    @Test
    void hashRoundtrip() {
        redisOps.hSet("h", "f1", new Demo("b", 2), Duration.ofMinutes(1));

        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(hashOps).put(eq("h"), eq("f1"), value.capture());
        assertTrue(value.getValue().toString().contains("\"name\":\"b\""));
        verify(redisTemplate).expire(eq("h"), any(Duration.class));

        when(hashOps.get("h", "f1")).thenReturn("{\"name\":\"b\",\"age\":2}");
        assertEquals("b", redisOps.hGet("h", "f1", Demo.class).name);
    }

    @Test
    void deleteVarargsAndCollection() {
        when(redisTemplate.delete(anyCollection())).thenReturn(2L);
        assertEquals(2, redisOps.delete("a", "b"));
    }

    @Test
    void setIfAbsentPassesThrough() {
        when(valueOps.setIfAbsent(eq("k"), anyString(), any(Duration.class))).thenReturn(Boolean.TRUE);
        assertTrue(redisOps.setIfAbsent("k", "v", Duration.ofSeconds(10)));
        assertThrows(IllegalArgumentException.class, () -> redisOps.setIfAbsent("k", "v", null));
    }
}
