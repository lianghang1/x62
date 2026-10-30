package com.scaffold.starter.observability.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 可观测性自动装配：公共标签 + 内建指标门面。
 *
 * <p>公共标签 {@code application} 取 {@code spring.application.name}（可经
 * {@code scaffold.observability.application-name} 覆盖），Prometheus 侧多应用
 * 聚合查询时用它区分实例归属；HTTP（http.server.requests）等 Micrometer 内建
 * 指标由 actuator 自动提供，无需额外注册。</p>
 *
 * <p>框架内建埋点约定（各 starter 以 optional micrometer 依赖各自打点，
 * 本 starter 不在场时自动静默）：
 * <ul>
 *   <li>job：scaffold.job.executions / scaffold.job.duration</li>
 *   <li>protection：scaffold.protection.abuse.rejected</li>
 *   <li>security：scaffold.security.login.failures</li>
 * </ul></p>
 *
 * @author lianghang
 */
@ConditionalOnProperty(prefix = "scaffold.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "scaffoldCommonTagsCustomizer")
    public MeterRegistryCustomizer<MeterRegistry> scaffoldCommonTagsCustomizer(
            ObservabilityProperties properties, Environment environment) {
        String appName = properties.getApplicationName() != null && !properties.getApplicationName().isEmpty()
                ? properties.getApplicationName()
                : environment.getProperty("spring.application.name", "scaffold");
        return registry -> registry.config().commonTags("application", appName);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScaffoldMetrics scaffoldMetrics(MeterRegistry registry) {
        return new ScaffoldMetrics(registry);
    }
}
