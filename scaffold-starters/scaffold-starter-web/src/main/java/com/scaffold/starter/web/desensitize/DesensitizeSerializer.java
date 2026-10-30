package com.scaffold.starter.web.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.util.Objects;

/**
 * 脱敏序列化器：经 {@link Desensitize} 元注解挂到 @JsonSerialize，属性级 ContextualSerializer，
 * 无注解的字段走默认序列化，零侵入。
 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private static final String MASK = "****";

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
        if (value == null || value.isEmpty() || type == null) {
            return value;
        }
        switch (type) {
            case PHONE:
                return value.length() <= 7 ? MASK
                        : value.substring(0, 3) + MASK + value.substring(value.length() - 4);
            case EMAIL: {
                int at = value.indexOf('@');
                return at <= 0 ? MASK
                        : value.charAt(0) + MASK + value.substring(at);
            }
            case ID_CARD:
                return value.length() <= 10 ? MASK
                        : value.substring(0, 6) + MASK + value.substring(value.length() - 4);
            case BANK_CARD:
                return value.length() <= 8 ? MASK
                        : value.substring(0, 4) + MASK + value.substring(value.length() - 4);
            case NAME:
                return value.length() == 1 ? MASK
                        : value.charAt(0) + MASK;
            case CUSTOM:
            default: {
                int prefix = Math.min(prefixKeep, value.length());
                int suffix = value.length() - prefix >= suffixKeep ? suffixKeep : 0;
                return value.substring(0, prefix) + MASK
                        + (suffix > 0 ? value.substring(value.length() - suffix) : "");
            }
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
