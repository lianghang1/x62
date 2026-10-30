package com.scaffold.starter.message.config;

import com.scaffold.starter.message.DefaultNotificationService;
import com.scaffold.starter.message.NotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 消息通道的自动装配类。
 *
 * <p>装配默认的 NotificationService：sms4j 存在则接入其 SmsBlend，
 * 不存在则短信通道自动降级为日志告警——可选依赖按 classpath 探测，业务零配置。</p>
 *
 * @author lianghang
 */
@Configuration
public class MessageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(NotificationService.class)
    public NotificationService notificationService(JavaMailSender mailSender,
                                                   ApplicationContext applicationContext) {
        Object smsBlend = null;
        try {
            Class<?> blendClass = Class.forName("org.dromara.sms4j.api.SmsBlend");
            smsBlend = applicationContext.getBeansOfType(blendClass).values().stream()
                    .findFirst().orElse(null);
        } catch (ClassNotFoundException ignored) {
            // sms4j 未引入：短信通道降级
        }
        return new DefaultNotificationService(mailSender, smsBlend);
    }
}
