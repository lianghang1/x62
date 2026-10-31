package com.scaffold.starter.security;

import com.scaffold.starter.security.openapi.SignUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 签名算法工具测试：标准向量、常量时间比较、null 安全。
 *
 * @author lianghang
 */
class SignUtilsTest {

    @Test
    void sha256ShouldMatchKnownVector() {
        // SHA-256("abc") 标准测试向量
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                SignUtils.sha256Hex("abc"));
        // 空串
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                SignUtils.sha256Hex(""));
    }

    @Test
    void hmacShouldMatchKnownVector() {
        // HMAC-SHA256(key="key", msg="The quick brown fox jumps over the lazy dog") 标准向量
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                SignUtils.hmacSha256Hex("The quick brown fox jumps over the lazy dog", "key"));
    }

    @Test
    void constantTimeEqualsShouldHandleNullAndUnequal() {
        assertTrue(SignUtils.constantTimeEquals("abc", "abc"));
        assertFalse(SignUtils.constantTimeEquals("abc", "abd"));
        assertFalse(SignUtils.constantTimeEquals(null, "abc"));
        assertFalse(SignUtils.constantTimeEquals("abc", null));
    }
}
