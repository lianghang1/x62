package com.scaffold.starter.protection.config;

import com.scaffold.starter.protection.idempotent.IdempotentAspect;
import com.scaffold.starter.protection.lock.DistributedLockAspect;
import com.scaffold.starter.protection.lock.RedisLockUtil;
import com.scaffold.starter.protection.ratelimit.RateLimitAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 服务保护自动装配：限流 / 幂等 / 分布式锁三个切面。
 */
@Configuration
public class ProtectionAutoConfiguration {

    @Bean
    public RedisLockUtil redisLockUtil(StringRedisTemplate redisTemplate) {
        return new RedisLockUtil(redisTemplate);
    }

    @Bean
    public RateLimitAspect rateLimitAspect(StringRedisTemplate redisTemplate) {
        return new RateLimitAspect(redisTemplate);
    }

    @Bean
    public IdempotentAspect idempotentAspect(StringRedisTemplate redisTemplate) {
        return new IdempotentAspect(redisTemplate);
    }

    @Bean
    public DistributedLockAspect distributedLockAspect(RedisLockUtil lockUtil) {
        return new DistributedLockAspect(lockUtil);
    }
}
