package com.scaffold.starter.security;

import com.scaffold.starter.security.captcha.CaptchaService;
import com.scaffold.starter.security.config.SecurityProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 图形验证码测试：触发策略（off/always/after_fail）与一次性消费语义。
 *
 * @author lianghang
 */
class CaptchaServiceTest {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    @BeforeAll
    static void headless() {
        // CI/容器环境无 X11：图片生成走 headless AWT（BufferedImage 无需显示服务器）
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private CaptchaService newService(String mode) {
        SecurityProperties.Captcha config = new SecurityProperties.Captcha();
        config.setMode(mode);
        config.setFailThreshold(3);
        return new CaptchaService(redisTemplate, config);
    }

    @Test
    void modeOffNeverRequires() {
        CaptchaService service = newService("off");
        assertFalse(service.required(0));
        assertFalse(service.required(100));
    }

    @Test
    void afterFailTriggersAtThreshold() {
        CaptchaService service = newService("after_fail");
        assertFalse(service.required(2));
        assertTrue(service.required(3));
    }

    @Test
    void alwaysAlwaysRequires() {
        CaptchaService service = newService("always");
        assertTrue(service.required(0));
    }

    @Test
    void verifyIsOneTimeConsume() {
        CaptchaService service = newService("always");
        // Redis 中存的是算式本身
        when(valueOps.get("scaffold:security:captcha:id1")).thenReturn("3+5");

        assertTrue(service.verifyAndConsume("id1", "8"));
        // 一次性：第二次即使 Redis 仍有值也不该有（此处模拟已删除）
        when(valueOps.get("scaffold:security:captcha:id1")).thenReturn(null);
        assertFalse(service.verifyAndConsume("id1", "8"));
    }

    @Test
    void nonNumericInputRejected() {
        CaptchaService service = newService("always");
        when(valueOps.get("scaffold:security:captcha:id2")).thenReturn("12+34");

        // 非数字输入（OCR 识别出的字符）一律拒绝——计算型的核心价值
        assertFalse(service.verifyAndConsume("id2", "abcd"));
        assertFalse(service.verifyAndConsume("id2", "46x"));
        // 错误答案拒绝
        assertFalse(service.verifyAndConsume("id2", "45"));
    }

    @Test
    void generateStoresWithTtl() {
        CaptchaService service = newService("always");
        Map<String, String> result = service.generate();

        assertFalse(result.get("captchaId").isEmpty());
        assertTrue(result.get("imageBase64").startsWith("data:image"));
        Mockito.verify(valueOps).set(anyString(), anyString(), any(Duration.class));
    }
}
