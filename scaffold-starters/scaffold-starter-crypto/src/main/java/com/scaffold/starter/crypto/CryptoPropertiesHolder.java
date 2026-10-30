package com.scaffold.starter.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 加密配置属性，绑定前缀 {@code scaffold.crypto}。
 *
 * <p>仅两项：算法类型（默认 {@code AES}，信创环境可切 {@code SM4}）与加密口令。
 * 口令用于派生实际密钥，必须通过环境变量注入，禁止硬编码在配置文件里。</p>
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "scaffold.crypto")
public class CryptoPropertiesHolder {

    /** 算法类型：AES（默认）| SM4（国密/信创）。 */
    private String type = "AES";

    /** 加密口令（派生密钥用），必须环境变量注入，禁止硬编码。 */
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
