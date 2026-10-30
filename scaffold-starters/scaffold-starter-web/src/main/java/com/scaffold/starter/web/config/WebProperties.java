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

    /** XSS 过滤（默认开启，参数与 body 尖括号转义） */
    private Xss xss = new Xss();

    public static class Xss {

        /** 是否启用 XSS 过滤 */
        private boolean enabled = true;

        /** 排除路径（Ant 风格，如富文本编辑接口 /internal/rich/**） */
        private List<String> excludeUrls = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getExcludeUrls() {
            return excludeUrls;
        }

        public void setExcludeUrls(List<String> excludeUrls) {
            this.excludeUrls = excludeUrls;
        }
    }

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

    public Xss getXss() {
        return xss;
    }

    public void setXss(Xss xss) {
        this.xss = xss;
    }

    public boolean isAccessLogEnabled() {
        return accessLogEnabled;
    }

    public void setAccessLogEnabled(boolean accessLogEnabled) {
        this.accessLogEnabled = accessLogEnabled;
    }
}
