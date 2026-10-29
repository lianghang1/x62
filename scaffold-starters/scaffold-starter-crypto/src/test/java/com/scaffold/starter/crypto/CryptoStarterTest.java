package com.scaffold.starter.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 加密组件单元测试：AES/SM4 往返 + 随机 IV 语义 + SM3 标准测试向量。
 */
class CryptoStarterTest {

    private static final String SECRET = "unit-test-secret";
    private static final String PLAIN = "13800001111|身份证 110101199001011234|app-secret-123";

    @Test
    void aesRoundTrip() {
        AesDataCipher cipher = new AesDataCipher(SECRET);
        String encrypted = cipher.encrypt(PLAIN);
        assertNotEquals(PLAIN, encrypted);
        // 随机 IV：同明文两次加密密文不同
        assertNotEquals(encrypted, cipher.encrypt(PLAIN));
        assertEquals(PLAIN, cipher.decrypt(encrypted));
    }

    @Test
    void sm4RoundTrip() {
        Sm4DataCipher cipher = new Sm4DataCipher(SECRET);
        String encrypted = cipher.encrypt(PLAIN);
        assertNotEquals(PLAIN, encrypted);
        assertNotEquals(encrypted, cipher.encrypt(PLAIN));
        assertEquals(PLAIN, cipher.decrypt(encrypted));
    }

    @Test
    void aesWrongKeyShouldFail() {
        String encrypted = new AesDataCipher(SECRET).encrypt(PLAIN);
        assertThrows(IllegalStateException.class,
                () -> new AesDataCipher("another-key").decrypt(encrypted));
    }

    @Test
    void sm3StandardVector() {
        // GB/T 32905-2016 标准测试向量：SM3("abc")
        assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0",
                Sm3DigestUtils.digestHex("abc"));
    }
}
