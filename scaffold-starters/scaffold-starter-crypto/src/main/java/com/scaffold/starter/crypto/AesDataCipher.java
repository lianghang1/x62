package com.scaffold.starter.crypto;

import com.scaffold.core.crypto.DataCipher;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 实现（默认）：JDK 自带，无需额外依赖。
 *
 * <p>格式：Base64(12 字节随机 IV || GCM 密文(含 128 位认证标签))——
 * 每次加密 IV 随机，同明文两次加密结果不同；密文被篡改时解密直接抛异常（完整性校验）。
 *
 * <p>密钥：对配置口令做 SHA-256 得到 32 字节 AES-256 密钥；口令必须走环境变量注入。
 */
public class AesDataCipher implements DataCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public AesDataCipher(String secret) {
        this.keySpec = new SecretKeySpec(sha256(secret), "AES");
    }

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 加密失败", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        try {
            byte[] all = Base64.getDecoder().decode(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec,
                    new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 解密失败（密文损坏或密钥不一致）", e);
        }
    }

    static byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
