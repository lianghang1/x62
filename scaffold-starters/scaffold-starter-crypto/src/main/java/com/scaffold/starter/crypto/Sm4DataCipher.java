package com.scaffold.starter.crypto;

import com.scaffold.core.crypto.DataCipher;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SM4-CBC-PKCS7 实现（国密/信创）：BouncyCastle 轻量 API，无需注册 Provider。
 *
 * <p>格式：Base64(16 字节随机 IV || SM4 密文)。SM4 密钥固定 128 位。
 *
 * <p>密钥：对配置口令做 SHA-256 取前 16 字节；口令必须走环境变量注入。
 */
public class Sm4DataCipher implements DataCipher {

    private static final int IV_LENGTH = 16;

    private final byte[] keyBytes;
    private final SecureRandom random = new SecureRandom();

    public Sm4DataCipher(String secret) {
        byte[] digest = AesDataCipher.sha256(secret);
        this.keyBytes = new byte[IV_LENGTH];
        System.arraycopy(digest, 0, keyBytes, 0, IV_LENGTH);
    }

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            byte[] encrypted = sm4(true, iv, plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("SM4 加密失败", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        try {
            byte[] all = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            byte[] plain = sm4(false, iv, java.util.Arrays.copyOfRange(all, IV_LENGTH, all.length));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("SM4 解密失败（密文损坏或密钥不一致）", e);
        }
    }

    private byte[] sm4(boolean forEncrypt, byte[] iv, byte[] input) throws Exception {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(new SM4Engine()), new PKCS7Padding());
        cipher.init(forEncrypt, new ParametersWithIV(new KeyParameter(keyBytes), iv));
        byte[] buffer = new byte[cipher.getOutputSize(input.length)];
        int len = cipher.processBytes(input, 0, input.length, buffer, 0);
        len += cipher.doFinal(buffer, len);
        byte[] result = new byte[len];
        System.arraycopy(buffer, 0, result, 0, len);
        return result;
    }
}
