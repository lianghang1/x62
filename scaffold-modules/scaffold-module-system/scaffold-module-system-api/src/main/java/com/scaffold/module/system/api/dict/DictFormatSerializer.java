package com.scaffold.module.system.api.dict;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.scaffold.common.util.SpringContextHolder;
import com.scaffold.module.system.api.api.DictApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * {@link DictFormat} 的 Jackson 序列化器：委托 {@code DictApi} 翻译，任何异常回退原值。
 *
 * <p>取 {@code DictApi} 走 {@link SpringContextHolder}——本类由注解触发实例化，
 * 不在容器管理内，静态出口是唯一可行通道；容器未就绪或 Bean 缺失时静默回退，
 * 保证「翻译失败不拖垮接口」。日志打 debug 级别：翻译是展示增强，刷屏无益。</p>
 *
 * @author lianghang
 */
public class DictFormatSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private static final Logger log = LoggerFactory.getLogger(DictFormatSerializer.class);

    private final String dictType;

    public DictFormatSerializer() {
        this(null);
    }

    DictFormatSerializer(String dictType) {
        this.dictType = dictType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(translateSafely(value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            DictFormat annotation = property.getAnnotation(DictFormat.class);
            if (annotation != null) {
                return new DictFormatSerializer(annotation.value());
            }
        }
        return prov.findValueSerializer(String.class, property);
    }

    private String translateSafely(String value) {
        if (value == null || dictType == null) {
            return value;
        }
        try {
            return SpringContextHolder.getBean(DictApi.class).translate(dictType, value);
        } catch (Exception e) {
            log.debug("[dict] 翻译回退原值: type={} value={} cause={}", dictType, value, e.getMessage());
            return value;
        }
    }
}
