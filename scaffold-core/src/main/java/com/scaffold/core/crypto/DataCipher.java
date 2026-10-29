package com.scaffold.core.crypto;

/**
 * 存储加密扩展点（技术横切 SPI）：
 * 敏感字段（手机号/证件号/appSecret/三方密钥等）入库前加密、出库后解密。
 *
 * <p>实现方：
 * <ul>
 *   <li>AES-256-GCM（默认）—— starter-crypto 的 {@code AesDataCipher}</li>
 *   <li>SM4-CBC（国密/信创）—— starter-crypto 的 {@code Sm4DataCipher}（BouncyCastle）</li>
 * </ul>
 *
 * <p>使用方式：实体字段上加 {@code @Convert(converter = EncryptedStringConverter.class)}
 * （starter-data-jpa 提供），启动时由 starter-crypto 自动装配并安装到 {@link DataCipherHolder}。
 * 业务代码也可以直接注入 DataCipher Bean 使用。
 */
public interface DataCipher {

    /** 明文 -> 密文（输出 Base64，含随机 IV，同明文每次加密结果不同） */
    String encrypt(String plaintext);

    /** 密文 -> 明文 */
    String decrypt(String ciphertext);
}
