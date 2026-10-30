package com.scaffold.starter.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * 消息通知的默认实现。
 *
 * <p>邮件用 JavaMailSender；短信为可选依赖 sms4j，存在 SmsBlend 时经反射接入
 * （避免编译期强依赖），不存在时降级为告警日志、消息丢弃但系统照常启动——
 * 通知是增强能力，不因其缺失阻断主流程。</p>
 *
 * @author lianghang
 */
public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final JavaMailSender mailSender;

    /** 发件人（spring.mail.username），可为空（本地调试） */
    @Autowired(required = false)
    private org.springframework.core.env.Environment environment;

    /** sms4j 的短信入口（可选依赖，反射桥接避免强依赖） */
    private final Object smsBlend;

    /** 短信防刷闸门（sms-guard 关闭时为 null） */
    private final SmsAbuseGuard smsAbuseGuard;

    public DefaultNotificationService(JavaMailSender mailSender, Object smsBlend,
                                      SmsAbuseGuard smsAbuseGuard) {
        this.mailSender = mailSender;
        this.smsBlend = smsBlend;
        this.smsAbuseGuard = smsAbuseGuard;
    }

    @Override
    public void sendSms(String phoneNumber, String templateId, String... params) {
        if (smsBlend == null) {
            log.warn("[notify] 短信通道未接入（未配置 sms4j 厂商），消息丢弃: phone={}", phoneNumber);
            return;
        }
        // 防刷：间隔/手机号日限/IP 日限/全局配额，任一超限抛 429，短信不发出
        if (smsAbuseGuard != null) {
            smsAbuseGuard.check(phoneNumber);
        }
        // sms4j SmsBlend.sendMessage(String phone, String templateId, LinkedHashMap<String,String>)
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < params.length; i += 2) {
            map.put(params[i], i + 1 < params.length ? params[i + 1] : "");
        }
        try {
            smsBlend.getClass()
                    .getMethod("sendMessage", String.class, String.class, java.util.LinkedHashMap.class)
                    .invoke(smsBlend, phoneNumber, templateId, map);
        } catch (Exception e) {
            throw new IllegalStateException("短信发送失败", e);
        }
    }

    @Override
    public void sendMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            String from = environment != null ? environment.getProperty("spring.mail.username") : null;
            if (from != null && !from.isEmpty()) {
                helper.setFrom(from);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("邮件发送失败", e);
        }
    }
}
