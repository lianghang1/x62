package com.scaffold.starter.security.policy;

import com.scaffold.starter.security.config.SecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 登录失败锁定（Redis 计数，RuoYi maxRetryCount 思路）：
 * 同一账号连续失败达到 maxFailCount 后锁定 lockSeconds；成功登录调用 reset 清零。
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
