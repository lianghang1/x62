package com.scaffold.starter.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SerialOps 单号生成测试：格式、位宽、首号 TTL、跨日 key、参数校验。
 *
 * @author lianghang
 */
class SerialOpsTest {

    private RedisOps redisOps;
    private SerialOps serialOps;

    @BeforeEach
    void setUp() {
        redisOps = mock(RedisOps.class);
        serialOps = new SerialOps(redisOps);
    }

    private String today() {
        return LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @Test
    void firstSerialGetsTtl() {
        when(redisOps.increment(any(), anyLong())).thenReturn(1L);

        String serial = serialOps.next("ORD");

        assertEquals("ORD" + today() + "-0001", serial);
        verify(redisOps).increment(eq("scaffold:serial:ORD:" + today()), eq(1L));
        verify(redisOps).expire(eq("scaffold:serial:ORD:" + today()), any(Duration.class));
    }

    @Test
    void subsequentSerialSkipsTtl() {
        when(redisOps.increment(any(), anyLong())).thenReturn(7L);

        String serial = serialOps.next("PAY");

        assertEquals("PAY" + today() + "-0007", serial);
        // 非首号不再补 TTL
        verify(redisOps, never()).expire(any(), any(Duration.class));
    }

    @Test
    void customSeqLengthPadsAndOverflowsNaturally() {
        when(redisOps.increment(any(), anyLong())).thenReturn(123456L);

        // 3 位位宽装不下 → 自然变长不截断
        assertEquals("REFUND" + today() + "-123456", serialOps.next("REFUND", 3));

        when(redisOps.increment(any(), anyLong())).thenReturn(2L);
        assertEquals("X" + today() + "-000002", serialOps.next("X", 6));
    }

    @Test
    void invalidArgumentsRejected() {
        assertThrows(IllegalArgumentException.class, () -> serialOps.next(" "));
        assertThrows(IllegalArgumentException.class, () -> serialOps.next(null));
        assertThrows(IllegalArgumentException.class, () -> serialOps.next("ORD", 0));
        assertThrows(IllegalArgumentException.class, () -> serialOps.next("ORD", 19));
    }

    @Test
    void serialFormatContract() {
        when(redisOps.increment(any(), anyLong())).thenReturn(42L);
        String serial = serialOps.next("ORD");
        // 契约：前缀 + 8 位日期 + '-' + 4 位序列
        assertTrue(serial.matches("ORD\\d{8}-\\d{4}"), "实际=" + serial);
    }
}
