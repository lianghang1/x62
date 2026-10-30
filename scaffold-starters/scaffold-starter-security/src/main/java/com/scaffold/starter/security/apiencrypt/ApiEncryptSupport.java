package com.scaffold.starter.security.apiencrypt;

import com.scaffold.common.crypto.DataCipherHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * {@code @ApiEncrypt} 的公共支撑：注解查找与密文体包装。
 *
 * @author lianghang
 */
final class ApiEncryptSupport {

    private ApiEncryptSupport() {
    }

    /** 解密请求体：整包读入 → DataCipher 解密 → 以明文重建输入流（headers 原样透传）。 */
    static HttpInputMessage decrypt(HttpInputMessage inputMessage) throws IOException {
        byte[] cipher = readAll(inputMessage.getBody());
        String plaintext = DataCipherHolder.get().decrypt(new String(cipher, StandardCharsets.UTF_8));
        byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(plainBytes);
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    /** 读取流全部字节（请求体一次性整包，与 CachedBodyRequestWrapper 语义一致）。 */
    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}
