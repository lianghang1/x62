package com.scaffold.starter.message;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息通道配置项（前缀 {@code scaffold.message}）。
 *
 * <p>目前承载短信防刷阈值；后续邮件限频、模板开关等在此扩展。</p>
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "scaffold.message")
public class MessageProperties {

    /** 短信防刷配置 */
    private SmsAbuseGuard.Config smsGuard = new SmsAbuseGuard.Config();

    public SmsAbuseGuard.Config getSmsGuard() {
        return smsGuard;
    }

    public void setSmsGuard(SmsAbuseGuard.Config smsGuard) {
        this.smsGuard = smsGuard;
    }
}
