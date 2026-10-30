package com.scaffold.starter.web.desensitize;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON 输出脱敏：标注在 DTO 的 String 字段上，序列化时自动打码（日志/响应全链路生效）。
 *
 * <pre>
 * &#64;Desensitize(DesensitizeType.PHONE)
 * private String phone;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {

    DesensitizeType value();

    /** CUSTOM 类型保留的前缀位数 */
    int prefixKeep() default 2;

    /** CUSTOM 类型保留的后缀位数 */
    int suffixKeep() default 2;
}
