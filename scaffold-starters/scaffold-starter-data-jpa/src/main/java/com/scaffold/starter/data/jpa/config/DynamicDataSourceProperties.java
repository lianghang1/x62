package com.scaffold.starter.data.jpa.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态数据源配置项，前缀 {@code scaffold.datasource.dynamic}。
 *
 * <p>因以 {@code @Bean} 方式声明（非 {@code @ConfigurationProperties} 扫描），
 * IDE 补全提示由 additional-spring-configuration-metadata.json 手工维护。</p>
 *
 * @author lianghang
 */
public class DynamicDataSourceProperties {

    /**
     * 默认数据源名：无 {@code @TargetDataSource} 上下文时路由到这里，
     * 必须是 {@code datasources} 中的 key。
     */
    private String primary = "master";

    /**
     * 数据源名 -> 连接配置，至少配置两个（单库场景直接用 spring.datasource，无需本组件）。
     */
    private Map<String, Conf> datasources = new LinkedHashMap<>();

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public Map<String, Conf> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, Conf> datasources) {
        this.datasources = datasources;
    }

    /**
     * 单个数据源的连接配置，逐个透传给 {@link org.springframework.boot.jdbc.DataSourceBuilder}。
     */
    public static class Conf {

        private String url;

        private String username;

        private String password;

        private String driverClassName;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }
}
