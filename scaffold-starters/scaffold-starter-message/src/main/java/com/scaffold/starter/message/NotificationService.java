package com.scaffold.starter.message;

/**
 * 消息通知门面接口，业务只依赖本接口发送通知。
 *
 * <p>短信走 sms4j（厂商配置化切换），邮件走 spring-boot-starter-mail；
 * 后续钉钉、企微 webhook 按 NotifyChannel SPI 扩展——通道可插拔，调用方无感。</p>
 *
 * @author lianghang
 */
public interface NotificationService {

    /**
     * 发送短信
     *
     * @param phoneNumber 手机号
     * @param templateId  厂商模板 ID
     * @param params      模板参数（按序）
     */
    void sendSms(String phoneNumber, String templateId, String... params);

    /** 发送邮件（HTML 支持） */
    void sendMail(String to, String subject, String htmlContent);
}
