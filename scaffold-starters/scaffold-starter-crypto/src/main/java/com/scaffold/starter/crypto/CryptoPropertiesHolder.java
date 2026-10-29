package com.scaffold.starter.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 加密配置（前缀 scaffold.crypto）。
 */
@ConfigurationProperties(prefix = "scaffold.crypto")
public class CryptoPropertiesHolder {

    /** AES（默认）| SM4（国密/信创） */
    private String type = "AES";

    /** 加密口令（派生密钥用）。必须环境变量注入，禁止硬编码 */
    private String key;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
