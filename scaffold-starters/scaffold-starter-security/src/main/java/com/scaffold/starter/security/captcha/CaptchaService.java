package com.scaffold.starter.security.captcha;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.GifCaptcha;
import cn.hutool.captcha.generator.MathGenerator;
import com.scaffold.starter.security.config.SecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * internal 登录计算型验证码服务（算式 GIF，Redis 一次性存储）。
 *
 * <p>计算型（如 {@code 3+5=?}）相比随机字符：OCR 打码平台识别的是图片字符，
 * 但算式结果无法从图片直接读出，必须完成一次「理解 + 计算」，脚本成本显著更高；
 * 对真人用户也无需辨认扭曲字符，输入数字即可。</p>
 *
 * <p>生成时随机 captchaId 对应图片 base64，Redis 存的是**算式本身**（不是答案），
 * 校验时由 {@link MathGenerator} 解析算式求值比对——用户输入任意非数字一律拒绝。
 * 取后即删（一次性），过期/不存在/不匹配一律拒绝。
 * 触发策略由 {@code scaffold.security.captcha.mode} 控制：
 * off（不启用）/ always（每次都要）/ after_fail（连续失败 failThreshold 次后强制）。</p>
 *
 * @author lianghang
 */
public class CaptchaService {

    private static final String KEY_PREFIX = com.scaffold.common.constant.RedisKeys.SECURITY_CAPTCHA;

    /** 无状态算式求值器（hutool 实现：解析四则运算并与用户输入比对） */
    private static final MathGenerator MATH = new MathGenerator();

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
     * 生成计算型验证码（算式 GIF 动图）。
     *
     * @return captchaId 与 base64 图片（{@code data:image/gif;base64,} 前缀，可直接进 img src）
     */
    public Map<String, String> generate() {
        // GIF 动图 + 算式生成器：120x40 画布，12 个干扰点
        GifCaptcha captcha = CaptchaUtil.createGifCaptcha(130, 40, MATH, 12);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        // 存算式而非答案：校验时用 MathGenerator 求值比对，答案不落库
        redisTemplate.opsForValue().set(KEY_PREFIX + captchaId, captcha.getCode(),
                Duration.ofSeconds(config.getTtlSeconds()));
        Map<String, String> result = new LinkedHashMap<>();
        result.put("captchaId", captchaId);
        // hutool 的 getImageBase64Data() 写死 png 前缀，实际产出 GIF，显式给对
        result.put("imageBase64", "data:image/gif;base64," + captcha.getImageBase64());
        return result;
    }

    /**
     * 校验并消费验证码（一次性：无论成败，验证码即失效）。
     *
     * @param captchaId 验证码 ID
     * @param code      用户输入的计算结果
     * @return true=有效
     */
    public boolean verifyAndConsume(String captchaId, String code) {
        if (captchaId == null || captchaId.isEmpty() || code == null || code.isEmpty()) {
            return false;
        }
        String key = KEY_PREFIX + captchaId;
        String expression = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return expression != null && MATH.verify(expression, code.trim());
    }
}
