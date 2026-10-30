package com.scaffold.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.crypto.DataCipher;
import com.scaffold.common.crypto.DataCipherHolder;
import com.scaffold.common.result.R;
import com.scaffold.starter.security.apiencrypt.ApiEncrypt;
import com.scaffold.starter.security.apiencrypt.ApiEncryptRequestBodyAdvice;
import com.scaffold.starter.security.apiencrypt.ApiEncryptResponseBodyAdvice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @ApiEncrypt Advice 测试：注解判定、请求解密、响应加密、往返一致性。
 *
 * @author lianghang
 */
class ApiEncryptAdviceTest {

    /** 逆序字符串假加密器：无状态可逆，专注验证 Advice 管线 */
    static class ReverseCipher implements DataCipher {
        @Override
        public String encrypt(String plaintext) {
            return new StringBuilder(plaintext).reverse().toString();
        }

        @Override
        public String decrypt(String ciphertext) {
            return new StringBuilder(ciphertext).reverse().toString();
        }
    }

    static class SampleController {

        @ApiEncrypt
        public R<String> encrypted(R<String> body) {
            return body;
        }

        public R<String> plain(R<String> body) {
            return body;
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiEncryptRequestBodyAdvice requestAdvice = new ApiEncryptRequestBodyAdvice();
    private final ApiEncryptResponseBodyAdvice responseAdvice =
            new ApiEncryptResponseBodyAdvice(objectMapper);

    @BeforeEach
    void setUp() {
        DataCipherHolder.install(new ReverseCipher());
    }

    @AfterEach
    void tearDown() {
        DataCipherHolder.install(null);
    }

    private MethodParameter encryptedParam() throws NoSuchMethodException {
        Method method = SampleController.class.getMethod("encrypted", R.class);
        return new MethodParameter(method, 0);
    }

    private MethodParameter plainParam() throws NoSuchMethodException {
        Method method = SampleController.class.getMethod("plain", R.class);
        return new MethodParameter(method, 0);
    }

    @Test
    void supportsOnlyAnnotatedEndpoints() throws NoSuchMethodException {
        assertTrue(requestAdvice.supports(encryptedParam(), R.class, StringHttpMessageConverter.class));
        assertFalse(requestAdvice.supports(plainParam(), R.class, StringHttpMessageConverter.class));
    }

    @Test
    void requestBodyDecrypted() throws Exception {
        String plaintext = "{\"code\":0}";
        String ciphertext = new ReverseCipher().encrypt(plaintext);
        MockHttpInputMessage input = new MockHttpInputMessage(ciphertext.getBytes(StandardCharsets.UTF_8));
        HttpInputMessage decrypted = requestAdvice.beforeBodyRead(input, encryptedParam(), R.class,
                StringHttpMessageConverter.class);
        byte[] body = readAll(decrypted.getBody());
        assertEquals(plaintext, new String(body, StandardCharsets.UTF_8));
    }

    @Test
    void responseBodyEncryptedAndRoundtrip() throws Exception {
        Method method = SampleController.class.getMethod("encrypted", R.class);
        MethodParameter returnType = new MethodParameter(method, -1);
        assertTrue(responseAdvice.supports(returnType, StringHttpMessageConverter.class));

        R<String> body = R.ok("secret-data");
        // request/response 均不参与加密逻辑，传 null 即可
        Object result = responseAdvice.beforeBodyWrite(body, returnType,
                org.springframework.http.MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                null, null);

        assertTrue(result instanceof String);
        String cipherText = (String) result;
        // 不是明文 JSON
        assertNotEquals(objectMapper.writeValueAsString(body), cipherText);
        // 客户端按同源密钥解密可得原响应
        String decrypted = new ReverseCipher().decrypt(cipherText);
        assertEquals(objectMapper.writeValueAsString(body), decrypted);
    }

    private byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
