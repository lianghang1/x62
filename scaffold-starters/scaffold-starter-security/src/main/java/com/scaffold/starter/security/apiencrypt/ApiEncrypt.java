package com.scaffold.starter.security.apiencrypt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口整体加解密：请求体密文进、响应体密文出。
 *
 * <p>标注在 Controller 方法或类上后，该接口的请求体为「DataCipher 加密的
 * 字符串」（明文为业务 JSON），响应体同样整包加密（含 R 包装）；加解密经
 * {@code DataCipher}（AES-256-GCM 或 SM4，由 starter-crypto 装配）。
 * 适用字段级脱敏不够、需整包防窃听的敏感接口（如密码改绑、证件提交）。</p>
 *
 * <p>与 {@code @ApiEncrypt} 配套的前端/客户端需持有同源密钥协商产物；
 * 密钥管理规约见 starter-crypto（环境变量注入，禁止硬编码）。</p>
 *
 * @author lianghang
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEncrypt {
}
