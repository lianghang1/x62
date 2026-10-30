package com.scaffold.starter.redis.config;

import com.scaffold.starter.redis.RedisOps;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 基建自动装配：提供 {@link RedisOps} 薄门面。
 *
 * <p>序列化策略不替换 Spring Boot 默认的 StringRedisTemplate（key/value 均为字符串），
 * JSON 序列化统一收敛在 RedisOps 内部走 {@code JsonUtils}——与 web 层同一 ObjectMapper，
 * 跨服务读写行为一致，也避免自定义 RedisTemplate 序列化器把其他组件的存量数据搞乱。</p>
 *
 * @author lianghang
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisOps redisOps(StringRedisTemplate redisTemplate) {
        return new RedisOps(redisTemplate);
    }
}
