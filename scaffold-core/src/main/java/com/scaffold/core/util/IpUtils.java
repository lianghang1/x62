package com.scaffold.core.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;

/**
 * 客户端 IP 解析工具。
 *
 * <p>取值优先级：{@code X-Forwarded-For} 首段（网关/代理链路的第一跳）→
 * {@code X-Real-IP}（Nginx 常用头）→ {@code remoteAddr}。代理头可被伪造，
 * 本工具只做取值不做防伪，防伪属于网关层的职责（在内网可信链路内使用）。</p>
 *
 * @author lianghang
 */
public final class IpUtils {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    /** 从请求解析客户端真实 IP（登录日志、操作日志、openapi 审计共用）。 */
    public static String clientIp(HttpServletRequest request) {
        return resolve(request.getHeader(HEADER_X_FORWARDED_FOR),
                request.getHeader(HEADER_X_REAL_IP),
                request.getRemoteAddr());
    }

    /** 从头部 Map 解析客户端 IP（供无 servlet 依赖的场景与单测使用）。 */
    public static String clientIp(Map<String, String> headers, String remoteAddr) {
        return resolve(headers.get(HEADER_X_FORWARDED_FOR),
                headers.get(HEADER_X_REAL_IP),
                remoteAddr);
    }

    /** 按 XFF 首段 → X-Real-IP → remoteAddr 的优先级取第一个有效值。 */
    private static String resolve(String forwardedFor, String realIp, String remoteAddr) {
        if (isValid(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        if (isValid(realIp)) {
            return realIp.trim();
        }
        return remoteAddr;
    }

    /** 头部值存在且不是 "unknown" 才视为有效（部分代理对缺失项回填 unknown）。 */
    private static boolean isValid(String value) {
        return value != null && !value.isEmpty() && !UNKNOWN.equalsIgnoreCase(value.trim().toLowerCase(Locale.ROOT));
    }
}
