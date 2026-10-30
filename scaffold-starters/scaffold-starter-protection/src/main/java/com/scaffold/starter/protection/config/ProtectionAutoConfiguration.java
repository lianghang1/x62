package com.scaffold.starter.protection.config;

import com.scaffold.starter.protection.abuse.AbuseGuard;
import com.scaffold.starter.protection.idempotent.IdempotentAspect;
import com.scaffold.starter.protection.lock.DistributedLockAspect;
import com.scaffold.starter.protection.lock.RedisLockUtil;
import com.scaffold.starter.protection.ratelimit.RateLimitAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 服务保护能力的自动装配类。
 *
 * <p>集中装配限流、幂等、分布式锁三个切面及其依赖的 RedisLockUtil——
 * 引入即生效，业务侧只需打注解，无需手动注册 Bean。</p>
 *
 * @author lianghang
 */
@Configuration
public class ProtectionAutoConfiguration {

    @Bean
    public RedisLockUtil redisLockUtil(StringRedisTemplate redisTemplate) {
        return new RedisLockUtil(redisTemplate);
    }

    @Bean
    public AbuseGuard abuseGuard(StringRedisTemplate redisTemplate) {
        return new AbuseGuard(redisTemplate);
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
