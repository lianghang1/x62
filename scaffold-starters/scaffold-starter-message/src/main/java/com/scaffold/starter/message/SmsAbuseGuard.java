package com.scaffold.starter.message;

import com.scaffold.core.util.IpUtils;
import com.scaffold.starter.protection.abuse.AbuseGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 短信防刷闸门：堵「短信轰炸」与配额盗刷的缺口。
 *
 * <p>短信是按条计费的外呼资源，被刷即烧钱 + 手机号被投诉。四道闸：
 * 同手机号发送间隔（验证码 60s 一条的行规）、同手机号日上限、
 * 同来源 IP 日上限（自动取当前请求的客户端 IP，非 HTTP 上下文跳过）、
 * 全局日上限（兜底配额，防止攻击者换号换 IP 打散）。全部经
 * {@link AbuseGuard} 落 Redis 固定窗口，超限抛 429。</p>
 *
 * @author lianghang
 */
public class SmsAbuseGuard {

    private static final Logger log = LoggerFactory.getLogger(SmsAbuseGuard.class);

    private static final String SCENE = "sms";

    private final AbuseGuard abuseGuard;
    private final Config config;

    /**
     * 防刷阈值集合（{@code scaffold.message.sms-guard}）。
     *
     * @author lianghang
     */
    public static class Config {

        /** 总开关；关闭后短信裸奔，仅建议本地调试关闭 */
        private boolean enabled = true;

        /** 同手机号两次发送最小间隔（秒） */
        private int phoneIntervalSeconds = 60;

        /** 同手机号每日发送上限 */
        private int phoneDailyLimit = 10;

        /** 同 IP 每日发送上限 */
        private int ipDailyLimit = 50;

        /** 全局每日发送上限（兜底配额） */
        private int globalDailyLimit = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPhoneIntervalSeconds() {
            return phoneIntervalSeconds;
        }

        public void setPhoneIntervalSeconds(int phoneIntervalSeconds) {
            this.phoneIntervalSeconds = phoneIntervalSeconds;
        }

        public int getPhoneDailyLimit() {
            return phoneDailyLimit;
        }

        public void setPhoneDailyLimit(int phoneDailyLimit) {
            this.phoneDailyLimit = phoneDailyLimit;
        }

        public int getIpDailyLimit() {
            return ipDailyLimit;
        }

        public void setIpDailyLimit(int ipDailyLimit) {
            this.ipDailyLimit = ipDailyLimit;
        }

        public int getGlobalDailyLimit() {
            return globalDailyLimit;
        }

        public void setGlobalDailyLimit(int globalDailyLimit) {
            this.globalDailyLimit = globalDailyLimit;
        }
    }

    public SmsAbuseGuard(AbuseGuard abuseGuard, Config config) {
        this.abuseGuard = abuseGuard;
        this.config = config;
    }

    /**
     * 发送前校验并计数：任一道闸超限抛 {@link com.scaffold.starter.protection.abuse.AbuseException}。
     *
     * @param phoneNumber 目标手机号
     */
    public void check(String phoneNumber) {
        if (!config.isEnabled()) {
            return;
        }
        // 1. 同手机号间隔闸（最常用、最先拦）
        abuseGuard.checkInterval(SCENE, phone(phoneNumber), config.getPhoneIntervalSeconds());
        // 2. 同手机号日额度
        abuseGuard.checkDaily(SCENE, phone(phoneNumber), config.getPhoneDailyLimit());
        // 3. 同 IP 日额度（web 请求上下文自动取 IP；定时任务等非 HTTP 上下文跳过）
        String ip = currentIp();
        if (ip != null) {
            abuseGuard.checkDaily(SCENE, "ip:" + ip, config.getIpDailyLimit());
        }
        // 4. 全局日配额兜底（防换号换 IP 打散）
        abuseGuard.checkDaily(SCENE, "global", config.getGlobalDailyLimit());
        log.debug("[notify] 短信防刷通过: phone={}, ip={}", phoneNumber, ip);
    }

    private String phone(String phoneNumber) {
        return "phone:" + phoneNumber;
    }

    /** 当前 HTTP 请求的客户端 IP；无请求上下文（定时任务/MQ 消费）返回 null。 */
    private String currentIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return IpUtils.clientIp(request);
    }
}
