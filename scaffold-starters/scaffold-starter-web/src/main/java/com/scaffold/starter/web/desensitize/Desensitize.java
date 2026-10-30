package com.scaffold.starter.web.desensitize;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON 输出脱敏注解，标注在 DTO 的 String 字段上即可在序列化时自动打码。
 *
 * <p>用法：</p>
 * <pre>
 * &#64;Desensitize(DesensitizeType.PHONE)
 * private String phone;
 * </pre>
 *
 * <p>作用于 Jackson 序列化层，日志、响应等全链路输出一并生效；{@code CUSTOM}
 * 类型通过 {@code prefixKeep/suffixKeep} 控制保留的前后缀位数。</p>
 *
 * @author lianghang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {

    DesensitizeType value();

    /** CUSTOM 类型保留的前缀位数。 */
    int prefixKeep() default 2;

    /** CUSTOM 类型保留的后缀位数。 */
    int suffixKeep() default 2;
}
