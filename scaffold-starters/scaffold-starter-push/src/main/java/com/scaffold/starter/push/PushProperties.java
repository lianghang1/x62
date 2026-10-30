package com.scaffold.starter.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 推送配置项（前缀 {@code scaffold.push}）。
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "scaffold.push")
public class PushProperties {

    /** starter 总开关 */
    private boolean enabled = true;

    /**
     * 多实例转发：部署多于一个节点时必须开启（Redis pub/sub 扇出），
     * 否则只投递到「收到推送请求的那个节点」上的连接。
     */
    private boolean multiInstance = false;

    /** Redis 转发频道名（多实例时各节点订阅同一频道） */
    private String relayTopic = "scaffold:push:relay";

    /** SSE emitter 超时（毫秒；0 表示永不超时，存活靠心跳） */
    private long sseTimeoutMs = 0L;

    /** 心跳间隔（秒；0 关闭心跳） */
    private int heartbeatSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMultiInstance() {
        return multiInstance;
    }

    public void setMultiInstance(boolean multiInstance) {
        this.multiInstance = multiInstance;
    }

    public String getRelayTopic() {
        return relayTopic;
    }

    public void setRelayTopic(String relayTopic) {
        this.relayTopic = relayTopic;
    }

    public long getSseTimeoutMs() {
        return sseTimeoutMs;
    }

    public void setSseTimeoutMs(long sseTimeoutMs) {
        this.sseTimeoutMs = sseTimeoutMs;
    }

    public int getHeartbeatSeconds() {
        return heartbeatSeconds;
    }

    public void setHeartbeatSeconds(int heartbeatSeconds) {
        this.heartbeatSeconds = heartbeatSeconds;
    }
}
