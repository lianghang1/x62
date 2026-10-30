package com.scaffold.starter.security.policy;

import com.scaffold.starter.security.config.SecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 基于 Redis 计数的登录失败锁定组件（RuoYi maxRetryCount 思路）。
 *
 * <p>同一账号连续失败达到 maxFailCount 后锁定 lockSeconds；成功登录调用 reset 清零——
 * 针对账号而非 IP，兼顾暴力破解防护与共享出口 IP 的误伤。</p>
 *
 * @author lianghang
 */
public class LoginFailureGuard {

    private static final String KEY_PREFIX = "scaffold:security:login:fail:";

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties.PasswordPolicy policy;

    public LoginFailureGuard(StringRedisTemplate redisTemplate,
                             SecurityProperties.PasswordPolicy policy) {
        this.redisTemplate = redisTemplate;
        this.policy = policy;
    }

    /** 账号是否已被临时锁定 */
    public boolean isLocked(String accountNo) {
        String count = redisTemplate.opsForValue().get(key(accountNo));
        return count != null && Integer.parseInt(count) >= policy.getMaxFailCount();
    }

    /** 记录一次失败，返回当前连续失败次数 */
    public int recordFailure(String accountNo) {
        Long count = redisTemplate.opsForValue().increment(key(accountNo));
        if (count != null && count == 1L) {
            redisTemplate.expire(key(accountNo), Duration.ofSeconds(policy.getLockSeconds()));
        }
        return count == null ? 0 : count.intValue();
    }

    /** 登录成功清零 */
    public void reset(String accountNo) {
        redisTemplate.delete(key(accountNo));
    }

    private String key(String accountNo) {
        return KEY_PREFIX + accountNo;
    }
}
