package com.scaffold.common;

import com.scaffold.common.util.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StringUtils 门面测试：lang3 静态穿透 + 自有方法委托 guava。
 *
 * @author lianghang
 */
class StringUtilsTest {

    @Test
    void lang3MethodsFlowThroughSubclass() {
        // 静态方法经子类穿透：继承即得全量 lang3 能力
        assertTrue(StringUtils.isBlank("  "));
        assertFalse(StringUtils.isBlank("x"));
        assertTrue(StringUtils.isEmpty(""));
        assertEquals("abc", StringUtils.defaultIfEmpty("", "abc"));
        assertEquals("ab...", StringUtils.abbreviate("abcdef", 5));
        assertNull(StringUtils.trimToNull("   "));
    }

    @Test
    void camelSnakeConvert() {
        assertEquals("user_name", StringUtils.camelToSnake("userName"));
        assertEquals("user_name", StringUtils.camelToSnake("UserName"));
        assertEquals("userName", StringUtils.snakeToCamel("user_name"));
        assertNull(StringUtils.camelToSnake(null));
        assertNull(StringUtils.snakeToCamel(null));
        // 往返一致
        assertEquals("orderItemId", StringUtils.snakeToCamel(StringUtils.camelToSnake("orderItemId")));
    }
}
