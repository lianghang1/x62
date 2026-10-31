package com.scaffold.starter.data.jpa.config;

import com.scaffold.starter.data.jpa.datasource.DataSourceContextHolder;
import com.scaffold.starter.data.jpa.datasource.DynamicDataSourceAspect;
import com.scaffold.starter.data.jpa.datasource.DynamicRoutingDataSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态数据源自动装配器，默认关闭，{@code scaffold.datasource.dynamic.enabled=true} 才生效。
 *
 * <p>启用后整体接管数据源：本装配注册在 {@link DataSourceAutoConfiguration} 之前，
 * 动态路由数据源一旦存在，Boot 按默认 {@code spring.datasource.*} 建库的逻辑自动退避。
 * 单库场景不要启用本组件——直接走默认装配即可，路由层是纯开销。</p>
 *
 * <p>装配三件套：路由数据源（含 primary 校验、每库独立连接池）+ 切面（最高优先级，
 * 先于事务开连接）+ ThreadLocal 持有器（静态工具类无需装配，此处仅注册切面）。</p>
 *
 * @author lianghang
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ConditionalOnProperty(prefix = "scaffold.datasource.dynamic", name = "enabled", havingValue = "true")
public class DynamicDataSourceAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "scaffold.datasource.dynamic")
    public DynamicDataSourceProperties dynamicDataSourceProperties() {
        return new DynamicDataSourceProperties();
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dynamicRoutingDataSource(DynamicDataSourceProperties properties) {
        Map<String, DynamicDataSourceProperties.Conf> confs = properties.getDatasources();
        if (confs.isEmpty()) {
            throw new IllegalStateException(
                    "已启用 scaffold.datasource.dynamic 但未配置任何数据源（scaffold.datasource.dynamic.datasources）");
        }
        String primary = properties.getPrimary();
        if (!confs.containsKey(primary)) {
            throw new IllegalStateException(
                    "默认数据源 [" + primary + "] 不在 datasources 配置中，可用: " + confs.keySet());
        }
        Map<Object, Object> targets = new LinkedHashMap<>();
        confs.forEach((name, conf) -> {
            DataSourceBuilder<?> builder = DataSourceBuilder.create()
                    .url(conf.getUrl())
                    .username(conf.getUsername())
                    .password(conf.getPassword());
            if (conf.getDriverClassName() != null) {
                builder.driverClassName(conf.getDriverClassName());
            }
            targets.put(name, builder.build());
        });
        DynamicRoutingDataSource routing = new DynamicRoutingDataSource(primary);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(targets.get(primary));
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean
    public DynamicDataSourceAspect dynamicDataSourceAspect(DynamicDataSourceProperties properties) {
        return new DynamicDataSourceAspect(properties);
    }
}
