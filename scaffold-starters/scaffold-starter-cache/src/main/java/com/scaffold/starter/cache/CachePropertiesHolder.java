package com.scaffold.starter.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存配置属性，绑定前缀 {@code scaffold.cache}。
 *
 * <p>控制两级缓存的形态与各层容量、时效：{@code type} 决定缓存实现
 * （默认 {@code two-level} 为 Caffeine + Redis，{@code redis} 为纯 Redis），
 * L1 追求低延迟小容量，L2 承担跨实例共享与较长 TTL。</p>
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "scaffold.cache")
public class CachePropertiesHolder {

    /** 缓存形态：two-level（默认）= Caffeine + Redis；redis = 纯 Redis。 */
    private String type = "two-level";

    /** L1 最大条目数。 */
    private long l1MaxSize = 4096;

    /** L1 过期时间（秒）。 */
    private long l1TtlSeconds = 60;

    /** L2 过期时间（秒）。 */
    private long l2TtlSeconds = 3600;

    /** Redis key 前缀。 */
    private String keyPrefix = "scaffold:cache:";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getL1MaxSize() {
        return l1MaxSize;
    }

    public void setL1MaxSize(long l1MaxSize) {
        this.l1MaxSize = l1MaxSize;
    }

    public long getL1TtlSeconds() {
        return l1TtlSeconds;
    }

    public void setL1TtlSeconds(long l1TtlSeconds) {
        this.l1TtlSeconds = l1TtlSeconds;
    }

    public long getL2TtlSeconds() {
        return l2TtlSeconds;
    }

    public void setL2TtlSeconds(long l2TtlSeconds) {
        this.l2TtlSeconds = l2TtlSeconds;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
