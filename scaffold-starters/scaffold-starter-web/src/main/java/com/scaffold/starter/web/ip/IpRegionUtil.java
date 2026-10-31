package com.scaffold.starter.web.ip;

import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

/**
 * IP 归属地查询工具（离线，基于 ip2region xdb 数据文件）。
 *
 * <p>xdb 数据随 jar 打包在 classpath，启动早期由 {@link IpRegionInitializer}
 * 一次性加载到内存（约 11MB），查询为纯内存二分，微秒级、无网络依赖。</p>
 *
 * <p>兜底语义：内网/回环地址返回 {@code 内网IP}；空值、非法值或查询异常
 * 返回空串——任何情况都不抛异常，日志记录不受影响。</p>
 *
 * <p>查询结果为 {@code 国家|区域|省|市|ISP} 五段竖线分隔串，如
 * {@code 中国|0|广东省|深圳市|电信}。</p>
 *
 * @author lianghang
 */
public final class IpRegionUtil {

    private static final Logger log = LoggerFactory.getLogger(IpRegionUtil.class);

    /** 内网/无法解析时的占位描述 */
    public static final String INTRANET = "内网IP";

    private static volatile Searcher searcher;

    private IpRegionUtil() {
    }

    /**
     * 加载 classpath 下的 ip2region.xdb 到内存，重复调用幂等。
     */
    public static synchronized void init() {
        if (searcher != null) {
            return;
        }
        try {
            long start = System.currentTimeMillis();
            byte[] bytes;
            try (java.io.InputStream in = IpRegionUtil.class.getClassLoader().getResourceAsStream("ip2region.xdb")) {
                if (in == null) {
                    log.warn("classpath 未找到 ip2region.xdb，IP 归属地查询不可用");
                    return;
                }
                bytes = StreamUtils.copyToByteArray(in);
            }
            searcher = Searcher.newWithBuffer(bytes);
            log.info("IP 归属地库(ip2region.xdb)加载完成，耗时 {}ms", System.currentTimeMillis() - start);
        } catch (Throwable e) {
            log.warn("IP 归属地库加载失败，getRegion 将始终返回空串: {}", e.getMessage());
        }
    }

    /**
     * 查询 IP 归属地。
     *
     * @param ip IP 地址（如 {@code 114.114.114.114}）
     * @return {@code 国家|区域|省|市|ISP} 格式串；内网返回 {@code 内网IP}；
     *         未初始化、空值、非法值或异常时返回空串
     */
    public static String getRegion(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return "";
        }
        String trimmed = ip.trim();
        if (isIntranet(trimmed)) {
            return INTRANET;
        }
        Searcher current = searcher;
        if (current == null) {
            return "";
        }
        try {
            return current.search(trimmed);
        } catch (Exception e) {
            log.debug("IP 归属地解析失败: {}", trimmed);
            return "";
        }
    }

    /** 是否内网/回环/链路本地地址 */
    static boolean isIntranet(String ip) {
        if ("127.0.0.1".equals(ip) || "0.0.0.0".equals(ip) || "::1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("169.254.")
                || ip.startsWith("fe80:") || ip.startsWith("fc") || ip.startsWith("fd")) {
            return true;
        }
        // 172.16.0.0/12：172.16 ~ 172.31
        if (ip.startsWith("172.")) {
            String second = ip.substring(4).split("\\.")[0];
            try {
                int seg = Integer.parseInt(second);
                return seg >= 16 && seg <= 31;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }
}
