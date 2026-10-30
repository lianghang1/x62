package com.scaffold.starter.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP 客户端配置项（前缀 {@code scaffold.http}）。
 *
 * <p>底层为 okhttp；需要对接特殊协议时通过自定义 {@code HttpTransport} Bean 替换，
 * 业务只面向 {@link com.scaffold.starter.http.ScaffoldHttpClient} 门面，零感知。</p>
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "scaffold.http")
public class HttpProperties {

    /** starter 总开关 */
    private boolean enabled = true;

    /** 建连超时（毫秒） */
    private int connectTimeoutMs = 5_000;

    /** 读超时（毫秒） */
    private int readTimeoutMs = 10_000;

    /** 幂等请求最大尝试次数（含首次） */
    private int retryMaxAttempts = 2;

    /** 重试退避间隔（毫秒） */
    private long retryBackoffMs = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public void setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }
}
