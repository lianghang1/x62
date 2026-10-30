package com.scaffold.starter.security.apiencrypt;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 请求体解密 Advice：{@code @ApiEncrypt} 接口的请求体整包解密后交反序列化。
 *
 * <p>密文为 UTF-8 字符串（DataCipher 输出 Base64 含随机 IV），明文为业务 JSON；
 * 解密失败（密文损坏/密钥不一致）由全局异常处理兜底返回统一错误。</p>
 *
 * @author lianghang
 */
@ControllerAdvice
public class ApiEncryptRequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasMethodAnnotation(ApiEncrypt.class)
                || methodParameter.hasParameterAnnotation(ApiEncrypt.class)
                || methodParameter.getContainingClass().isAnnotationPresent(ApiEncrypt.class);
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        return ApiEncryptSupport.decrypt(inputMessage);
    }
}
