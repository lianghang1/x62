package com.scaffold.common;

import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.common.util.BizAssert;
import com.scaffold.common.util.IpUtils;
import com.scaffold.common.util.JsonUtils;
import com.scaffold.common.util.MaskUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * core 工具类单测：脱敏/JSON/断言/IP 解析的行为基线。
 *
 * <p>JSON 用例重点锁三类语义：日期按 {@code yyyy-MM-dd HH:mm:ss} 文本往返、
 * 未知字段容忍、BigDecimal 精度不丢失——这些是各模块共享 ObjectMapper 后
 * 最容易被无意改坏的约定。</p>
 *
 * @author lianghang
 */
class CoreUtilsTest {

    @Test
    void maskAllTypes() {
        assertEquals("138****5678", MaskUtils.phone("13812345678"));
        assertEquals("a****@example.com", MaskUtils.email("alice@example.com"));
        assertEquals("110101****1234", MaskUtils.idCard("110101199001011234"));
        assertEquals("6222****1234", MaskUtils.bankCard("6222020200112341234"));
        assertEquals("张****", MaskUtils.name("张三"));
        assertEquals("ab****gh", MaskUtils.custom("abcdefgh", 2, 2));
        // 过短源串整体打码，null/空串原样返回
        assertEquals(MaskUtils.MASK, MaskUtils.phone("138"));
        assertNull(MaskUtils.phone(null));
        assertEquals("", MaskUtils.name(""));
    }

    @Test
    void jsonRoundTripWithDateAndTolerance() {
        Map<String, Object> source = new HashMap<>();
        source.put("time", LocalDateTime.of(2026, 10, 30, 8, 30, 0));
        source.put("amount", new BigDecimal("12.30"));

        String json = JsonUtils.toJson(source);
        assertTrue(json.contains("2026-10-30 08:30:00"), json);
        assertTrue(json.contains("12.30"), json);

        Map<String, Object> back = JsonUtils.toMap(json);
        assertEquals("2026-10-30 08:30:00", back.get("time"));
        // USE_BIG_DECIMAL_FOR_FLOATS：浮点统一 BigDecimal，精度不丢
        assertEquals(new BigDecimal("12.30"), back.get("amount"));

        // 未知字段容忍 + 空白输入返回 null
        assertTrue(JsonUtils.isValidJson("{\"a\":1}"));
        assertFalse(JsonUtils.isValidJson("{a:1}"));
        assertNull(JsonUtils.fromJson("  ", Map.class));
        assertNull(JsonUtils.toJson(null));
    }

    @Test
    void jsonTypeReferenceAndConvert() {
        String json = "[{\"id\":1},{\"id\":2}]";
        List<Map<String, Object>> list =
                JsonUtils.fromJson(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() { });
        assertEquals(2, list.size());

        Map<String, Object> from = new HashMap<>();
        from.put("id", 1);
        Point point = JsonUtils.convertValue(from, Point.class);
        assertEquals(1, point.id);
    }

    @Test
    void bizAssertThrowsBizException() {
        BizException ex = assertThrows(BizException.class, () -> BizAssert.param(false, "金额必须大于 0"));
        assertEquals(GlobalErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertEquals("金额必须大于 0", ex.getMessage());
        assertThrows(BizException.class, () -> BizAssert.notNull(null, GlobalErrorCode.NOT_FOUND));
        assertThrows(BizException.class, () -> BizAssert.notBlank(" ", GlobalErrorCode.BAD_REQUEST, "账号必填"));
        BizException fail = assertThrows(BizException.class, () -> BizAssert.fail(GlobalErrorCode.FORBIDDEN));
        assertEquals(GlobalErrorCode.FORBIDDEN, fail.getErrorCode());
    }

    @Test
    void ipResolutionPriority() {
        Map<String, String> headers = new HashMap<>();
        // XFF 多级代理取首段
        headers.put("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        assertEquals("10.0.0.1", IpUtils.clientIp(headers, "192.168.1.9"));
        // XFF 缺失时回退 X-Real-IP
        headers.remove("X-Forwarded-For");
        headers.put("X-Real-IP", "10.0.0.3");
        assertEquals("10.0.0.3", IpUtils.clientIp(headers, "192.168.1.9"));
        // 代理头为 unknown / 缺失时取 remoteAddr
        headers.put("X-Forwarded-For", "unknown");
        headers.remove("X-Real-IP");
        assertEquals("192.168.1.9", IpUtils.clientIp(headers, "192.168.1.9"));
    }

    /** convertValue 目标类型。 */
    public static class Point {
        public int id;
    }
}
