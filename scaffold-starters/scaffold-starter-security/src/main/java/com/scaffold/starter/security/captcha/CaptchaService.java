package com.scaffold.starter.security.captcha;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.scaffold.starter.security.config.SecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * internal 登录图形验证码服务（Hutool 实现，Redis 一次性存储）。
 *
 * <p>验证码防机器撞库：生成时随机 captchaId 对应图片 base64，答案落 Redis
 * 带 TTL；校验时取后即删（一次性），过期/不存在/不匹配一律拒绝。
 * 触发策略由 {@code scaffold.security.captcha.mode} 控制：
 * off（不启用）/ always（每次都要）/ after_fail（连续失败 failThreshold 次后强制）。</p>
 *
 * @author lianghang
 */
public class CaptchaService {

    private static final String KEY_PREFIX = "scaffold:security:captcha:";

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties.Captcha config;

    public CaptchaService(StringRedisTemplate redisTemplate, SecurityProperties.Captcha config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
    }

    /**
     * 是否需要验证码。
     *
     * @param failCount 该账号当前连续失败次数
     * @return true=登录时必须携带有效验证码
     */
    public boolean required(int failCount) {
        if ("always".equalsIgnoreCase(config.getMode())) {
            return true;
        }
        if ("after_fail".equalsIgnoreCase(config.getMode())) {
            return failCount >= config.getFailThreshold();
        }
        return false;
    }

    /**
     * 生成验证码。
     *
     * @return captchaId 与 base64 图片（含 {@code data:image/png;base64,} 前缀，可直接进 img src）
     */
    public Map<String, String> generate() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 14);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_PREFIX + captchaId, captcha.getCode(),
                Duration.ofSeconds(config.getTtlSeconds()));
        Map<String, String> result = new LinkedHashMap<>();
        result.put("captchaId", captchaId);
        result.put("imageBase64", captcha.getImageBase64Data());
        return result;
    }

    /**
     * 校验并消费验证码（一次性：无论成败，验证码即失效）。
     *
     * @param captchaId 验证码 ID
     * @param code      用户输入
     * @return true=有效
     */
    public boolean verifyAndConsume(String captchaId, String code) {
        if (captchaId == null || captchaId.isEmpty() || code == null || code.isEmpty()) {
            return false;
        }
        String key = KEY_PREFIX + captchaId;
        String expected = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return expected != null && expected.equalsIgnoreCase(code.trim());
    }
}
