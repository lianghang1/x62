package com.scaffold.starter.protection.config;

import com.scaffold.starter.protection.abuse.AbuseGuard;
import com.scaffold.starter.protection.idempotent.IdempotentAspect;
import com.scaffold.starter.protection.lock.DistributedLockAspect;
import com.scaffold.starter.protection.lock.RedisLockUtil;
import com.scaffold.starter.protection.ratelimit.RateLimitAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 服务保护能力的自动装配类。
 *
 * <p>集中装配限流、幂等、分布式锁三个切面及其依赖的 RedisLockUtil——
 * 引入即生效，业务侧只需打注解，无需手动注册 Bean。</p>
 *
 * <p>类路径无 Redis（如 server-lite 单机零依赖运行）时本装配整体跳过，
 * 各切面 Bean 不注册——限流/幂等/锁注解静默不生效，业务侧无需改动。</p>
 *
 * @author lianghang
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
public class ProtectionAutoConfiguration {

    @Bean
    public RedisLockUtil redisLockUtil(StringRedisTemplate redisTemplate) {
        return new RedisLockUtil(redisTemplate);
    }

    @Bean
    public AbuseGuard abuseGuard(StringRedisTemplate redisTemplate,
                                 org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistry) {
        return new AbuseGuard(redisTemplate, meterRegistry.getIfAvailable());
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
