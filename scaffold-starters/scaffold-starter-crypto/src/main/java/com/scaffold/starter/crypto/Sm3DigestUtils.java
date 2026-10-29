package com.scaffold.starter.crypto;

import org.bouncycastle.crypto.digests.SM3Digest;

import java.nio.charset.StandardCharsets;

/**
 * SM3 国密摘要工具（信创场景：密码哈希/报文摘要备选，与 BCrypt/SHA-256 并列可配）。
 */
public final class Sm3DigestUtils {

    private Sm3DigestUtils() {
    }

    /** SM3 摘要，输出 64 位小写 hex */
    public static String digestHex(String input) {
        SM3Digest digest = new SM3Digest();
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        digest.update(data, 0, data.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        StringBuilder sb = new StringBuilder(out.length * 2);
        for (byte b : out) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
