package com.scaffold.starter.web.desensitize;

import com.scaffold.common.util.StringUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.scaffold.common.util.MaskUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * 脱敏序列化器，按 {@link Desensitize} 注解的类型定义对字符串值打码。
 *
 * <p>通过 {@code @JacksonAnnotationsInside} 元注解挂到 {@code @JsonSerialize} 上，
 * 并实现属性级 {@code ContextualSerializer}：仅携带注解的 String 字段替换为打码
 * 版本，其余字段走默认序列化，对业务零侵入。</p>
 *
 * @author lianghang
 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType type;
    private int prefixKeep;
    private int suffixKeep;

    public DesensitizeSerializer() {
    }

    private DesensitizeSerializer(DesensitizeType type, int prefixKeep, int suffixKeep) {
        this.type = type;
        this.prefixKeep = prefixKeep;
        this.suffixKeep = suffixKeep;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(mask(value));
    }

    String mask(String value) {
        if (StringUtils.isEmpty(value) || type == null) {
            return value;
        }
        switch (type) {
            case PHONE:
                return MaskUtils.phone(value);
            case EMAIL:
                return MaskUtils.email(value);
            case ID_CARD:
                return MaskUtils.idCard(value);
            case BANK_CARD:
                return MaskUtils.bankCard(value);
            case NAME:
                return MaskUtils.name(value);
            case CUSTOM:
            default:
                return MaskUtils.custom(value, prefixKeep, suffixKeep);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws com.fasterxml.jackson.databind.JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }
        Desensitize annotation = property.getAnnotation(Desensitize.class);
        if (annotation != null && String.class.equals(property.getType().getRawClass())) {
            return new DesensitizeSerializer(annotation.value(),
                    annotation.prefixKeep(), annotation.suffixKeep());
        }
        return prov.findValueSerializer(property.getType(), property);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DesensitizeSerializer)) {
            return false;
        }
        DesensitizeSerializer that = (DesensitizeSerializer) o;
        return type == that.type && prefixKeep == that.prefixKeep && suffixKeep == that.suffixKeep;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, prefixKeep, suffixKeep);
    }
}
