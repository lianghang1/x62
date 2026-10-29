package com.scaffold.starter.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * starter-web 配置项（前缀 scaffold.web）。
 */
@ConfigurationProperties(prefix = "scaffold.web")
public class WebProperties {

    private Cors cors = new Cors();

    /** 访问日志切面开关 */
    private boolean accessLogEnabled = true;

    public static class Cors {

        /** 允许的跨域来源；为空表示不启用 CORS 配置（默认关闭，禁止全放开） */
        private List<String> allowedOrigins = new ArrayList<>();

        private List<String> allowedMethods = new ArrayList<>();

        private List<String> allowedHeaders = new ArrayList<>();

        /** 预检请求缓存时间（秒） */
        private long maxAge = 1800;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public boolean isAccessLogEnabled() {
        return accessLogEnabled;
    }

    public void setAccessLogEnabled(boolean accessLogEnabled) {
        this.accessLogEnabled = accessLogEnabled;
    }
}
