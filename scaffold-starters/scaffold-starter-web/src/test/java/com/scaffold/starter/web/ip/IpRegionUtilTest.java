package com.scaffold.starter.web.ip;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IP 归属地查询测试：公网/内网/非法 IP 三类语义与内网判定规则。
 *
 * @author lianghang
 */
class IpRegionUtilTest {

    @BeforeAll
    static void init() {
        IpRegionUtil.init();
    }

    @Test
    void testPublicIp() {
        // 公网 DNS：中国江苏南京（三方数据，宽松断言国家段）
        String region = IpRegionUtil.getRegion("114.114.114.114");
        assertTrue(region.startsWith("中国"), "实际返回: " + region);
        // 五段结构：国家|区域|省|市|ISP
        assertEquals(5, region.split("\\|").length);

        String google = IpRegionUtil.getRegion("8.8.8.8");
        assertTrue(google.contains("美国"), "实际返回: " + google);
    }

    @Test
    void testIntranetIp() {
        assertEquals("内网IP", IpRegionUtil.getRegion("127.0.0.1"));
        assertEquals("内网IP", IpRegionUtil.getRegion("192.168.1.100"));
        assertEquals("内网IP", IpRegionUtil.getRegion("10.0.0.1"));
        assertEquals("内网IP", IpRegionUtil.getRegion("172.16.0.1"));
        assertEquals("内网IP", IpRegionUtil.getRegion("172.31.255.255"));
        assertEquals("内网IP", IpRegionUtil.getRegion("::1"));
    }

    @Test
    void testInvalidIp() {
        assertEquals("", IpRegionUtil.getRegion(null));
        assertEquals("", IpRegionUtil.getRegion(""));
        assertEquals("", IpRegionUtil.getRegion("   "));
        assertEquals("", IpRegionUtil.getRegion("not-an-ip"));
        assertEquals("", IpRegionUtil.getRegion("999.999.999.999"));
    }

    @Test
    void testIsIntranetRule() {
        assertTrue(IpRegionUtil.isIntranet("172.16.0.1"));
        assertTrue(IpRegionUtil.isIntranet("172.31.0.1"));
        // 172.32+ 不属于 172.16/12 私有段
        assertFalse(IpRegionUtil.isIntranet("172.32.0.1"));
        assertTrue(IpRegionUtil.isIntranet("fe80::1"));
        assertTrue(IpRegionUtil.isIntranet("fd00::1"));
        assertFalse(IpRegionUtil.isIntranet("114.114.114.114"));
    }
}
