package com.scaffold.starter.crypto;

import com.scaffold.core.crypto.DataCipher;
import com.scaffold.core.crypto.DataCipherHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 加密自动装配：按 scaffold.crypto.type 装配 DataCipher 并安装到 Holder
 * （Holder 供 JPA AttributeConverter 等非 Spring Bean 场景读取）。
 *
 * <p>配置示例：
 * <pre>
 * scaffold:
 *   crypto:
 *     type: SM4            # 信创环境切国密；默认 AES
 *     key: ${SCAFFOLD_CRYPTO_KEY}
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(CryptoPropertiesHolder.class)
public class CryptoAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CryptoAutoConfiguration.class);

    /** application.yml 里的开发默认值；出现即说明未做环境变量注入 */
    private static final String DEV_DEFAULT_KEY = "dev-only-key-change-me";

    @Bean
    @ConditionalOnMissingBean(DataCipher.class)
    public DataCipher dataCipher(CryptoPropertiesHolder properties) {
        if (properties.getKey() == null || properties.getKey().trim().isEmpty()) {
            throw new IllegalStateException(
                    "scaffold.crypto.key 未配置：敏感字段加密需要密钥，请通过环境变量注入（参考 .env.example）");
        }
        if (DEV_DEFAULT_KEY.equals(properties.getKey())) {
            log.warn("[crypto] 正在使用开发默认密钥，密文不具备保密性——生产/信创环境必须设置 SCAFFOLD_CRYPTO_KEY 环境变量");
        }
        boolean sm4 = "SM4".equalsIgnoreCase(properties.getType());
        DataCipher cipher = sm4
                ? new Sm4DataCipher(properties.getKey())
                : new AesDataCipher(properties.getKey());
        DataCipherHolder.install(cipher);
        log.info("[crypto] DataCipher 已装配: {}（{} 位密钥）",
                sm4 ? "SM4-CBC 国密" : "AES-256-GCM", sm4 ? 128 : 256);
        return cipher;
    }
}
