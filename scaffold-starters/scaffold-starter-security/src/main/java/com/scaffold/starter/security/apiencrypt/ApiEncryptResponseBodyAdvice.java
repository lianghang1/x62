package com.scaffold.starter.security.apiencrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.crypto.DataCipherHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体加密 Advice：{@code @ApiEncrypt} 接口的响应整包（含 R 包装）加密为字符串。
 *
 * <p>写入顺序：业务对象 → ObjectMapper 序列化 → DataCipher 加密 → 密文字符串；
 * 客户端按同源密钥解密后得到统一响应体 JSON。</p>
 *
 * @author lianghang
 */
@ControllerAdvice
public class ApiEncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ApiEncryptResponseBodyAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(ApiEncrypt.class)
                || returnType.getContainingClass().isAnnotationPresent(ApiEncrypt.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        try {
            String json = body instanceof String
                    ? (String) body
                    : objectMapper.writeValueAsString(body);
            return DataCipherHolder.get().encrypt(json);
        } catch (Exception e) {
            throw new IllegalStateException("[api-encrypt] 响应加密失败: "
                    + returnType.getMethod(), e);
        }
    }
}
