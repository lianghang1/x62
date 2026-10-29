package com.scaffold.starter.security.openapi;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * openapi 签名算法（约定文档化，双方按此实现）：
 *
 * <pre>
 * bodyHash = SHA-256(请求体原文) 的 hex（GET/DELETE 请求体为空串）
 * signBase = appKey + "\n" + timestamp + "\n" + nonce + "\n" + bodyHash
 * sign     = HMAC-SHA256(signBase, appSecret) 的 hex
 * </pre>
 *
 * 防重放 = timestamp 窗口校验（默认 ±300s）+ nonce 一次性（Redis setIfAbsent，TTL=窗口）。
 */
public final class SignUtils {

    private SignUtils() {
    }

    public static String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    public static String hmacSha256Hex(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return hex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 不可用", e);
        }
    }

    /** 常量时间比较，防时序攻击 */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
