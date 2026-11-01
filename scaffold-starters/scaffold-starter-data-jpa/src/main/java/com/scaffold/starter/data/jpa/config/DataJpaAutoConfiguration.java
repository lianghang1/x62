package com.scaffold.starter.data.jpa.config;

import com.scaffold.starter.data.jpa.audit.ScaffoldAuditorHolder;
import com.scaffold.starter.data.jpa.datachange.DataChangeEventListener;
import com.scaffold.starter.data.jpa.datascope.DataScopeAspect;
import com.scaffold.starter.data.jpa.id.Snowflake;
import com.scaffold.starter.data.jpa.id.SnowflakeIdentifierGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import java.util.Optional;

/**
 * starter-data-jpa 自动装配器，提供 JPA Auditing 与雪花 ID 单例。
 *
 * <p>审计人通过 {@code AuditorAware} 从 {@code ScaffoldAuditorHolder} 读取，
 * 由安全层在认证后写入，data-jpa 自身不感知登录态；实体与 Repository 扫描
 * 范围统一为 {@code com.scaffold}，业务模块放各自包下即可被纳入。</p>
 *
 * <p>雪花 ID 以单例 Bean 构建并同步初始化到 {@code SnowflakeIdentifierGenerator}
 * 的静态引用——Hibernate 通过反射实例化生成器，拿不到 Spring 容器，只能走静态注入。</p>
 *
 * @author lianghang
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

    @Bean
    @ConditionalOnMissingBean(DataScopeAspect.class)
    public DataScopeAspect dataScopeAspect(PlatformTransactionManager transactionManager) {
        return new DataScopeAspect(transactionManager);
    }

    /**
     * 数据变更留痕监听器：注册 Hibernate POST_COMMIT 事件组，
     * 对 {@code @DataChange} 实体在事务提交后发布变更事件。
     * {@code scaffold.data.data-change-enabled=false} 可整体关闭。
     */
    @Bean
    @ConditionalOnProperty(prefix = "scaffold.data", name = "data-change-enabled",
            havingValue = "true", matchIfMissing = true)
    public DataChangeEventListener dataChangeEventListener(EntityManagerFactory entityManagerFactory) {
        return new DataChangeEventListener(entityManagerFactory);
    }
}
