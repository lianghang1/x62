package com.scaffold.starter.data.jpa.config;

import com.scaffold.starter.data.jpa.audit.ScaffoldAuditorHolder;
import com.scaffold.starter.data.jpa.id.Snowflake;
import com.scaffold.starter.data.jpa.id.SnowflakeIdentifierGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

/**
 * starter-data-jpa 自动装配：
 * JPA Auditing（审计人取 ScaffoldAuditorHolder，由安全层写入）+ 雪花 ID 单例。
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "scaffoldAuditorAware")
@EntityScan(basePackages = "com.scaffold")
@EnableJpaRepositories(basePackages = "com.scaffold")
public class DataJpaAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "scaffold.data")
    public SnowflakeProperties snowflakeProperties() {
        return new SnowflakeProperties();
    }

    @Bean
    public Snowflake snowflake(SnowflakeProperties properties) {
        Snowflake snowflake = new Snowflake(properties.getWorkerId(), properties.getDatacenterId());
        SnowflakeIdentifierGenerator.init(snowflake);
        return snowflake;
    }

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> scaffoldAuditorAware() {
        return () -> Optional.ofNullable(ScaffoldAuditorHolder.get());
    }
}
