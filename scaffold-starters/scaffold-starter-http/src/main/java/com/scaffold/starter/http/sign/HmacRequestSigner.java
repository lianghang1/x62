package com.scaffold.starter.http.sign;

import com.scaffold.starter.http.transport.TransportRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * HMAC-SHA256 请求签名器（与自家 openapi 网关对齐的默认实现）。
 *
 * <p>签名要素：时间戳 + nonce 防重放，对
 * {@code method\npath\ntimestamp\nnonce\nbodySha256} 计算 HMAC，
 * 通过 {@code X-Sign-*} 系列头随请求下发；网关按同算法校验。
 * 微信支付、支付宝等私有协议在业务模块实现 {@link RequestSigner} 即可。</p>
 *
 * @author lianghang
 */
public class HmacRequestSigner implements RequestSigner {

    private static final String HEADER_APP_KEY = "X-Sign-App-Key";
    private static final String HEADER_TIMESTAMP = "X-Sign-Timestamp";
    private static final String HEADER_NONCE = "X-Sign-Nonce";
    private static final String HEADER_SIGNATURE = "X-Sign-Signature";

    private final String appKey;
    private final byte[] secret;

    public HmacRequestSigner(String appKey, String secret) {
        this.appKey = appKey;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public TransportRequest sign(TransportRequest request) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String path = request.getUrl().replaceAll("^https?://[^/]+", "");
        String bodySha256 = sha256Hex(request.getBody() == null ? new byte[0] : request.getBody());
        String stringToSign = request.getMethod() + "\n" + path + "\n"
                + timestamp + "\n" + nonce + "\n" + bodySha256;

        return request.withHeader(HEADER_APP_KEY, appKey)
                .withHeader(HEADER_TIMESTAMP, timestamp)
                .withHeader(HEADER_NONCE, nonce)
                .withHeader(HEADER_SIGNATURE, hmacSha256Hex(stringToSign));
    }

    /** SHA-256 摘要（十六进制小写）。 */
    private static String sha256Hex(byte[] data) {
        return hex(digest("SHA-256", data));
    }

    /** HMAC-SHA256 摘要（十六进制小写）。 */
    private String hmacSha256Hex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return hex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    /** 通用摘要计算。 */
    private static byte[] digest(String algorithm, byte[] data) {
        try {
            return MessageDigest.getInstance(algorithm).digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(algorithm + " 计算失败", e);
        }
    }

    /** 字节数组转十六进制小写（JDK8 无 HexFormat，自行实现）。 */
    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
