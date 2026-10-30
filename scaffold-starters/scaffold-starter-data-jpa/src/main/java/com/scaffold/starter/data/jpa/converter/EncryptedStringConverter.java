package com.scaffold.starter.data.jpa.converter;

import com.scaffold.common.crypto.DataCipherHolder;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * 敏感字段加密转换器，实体字段声明即接入存储加密。
 *
 * <p>用法：</p>
 * <pre>
 * &#64;Convert(converter = EncryptedStringConverter.class)
 * &#64;Column(name = "phone", length = 256)
 * private String phone;
 * </pre>
 *
 * <p>加密后为 Base64 密文，列长需按密文长度放宽（建议原长度的 2~4 倍或
 * VARCHAR(256) 起）。加密器由 starter-crypto 启动时安装到 Holder——未引入
 * 该 starter 时，启动后首次读写会报明确错误，避免静默明文落库。</p>
 *
 * @author lianghang
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        return DataCipherHolder.get().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        return DataCipherHolder.get().decrypt(dbData);
    }
}
