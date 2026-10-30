package com.scaffold.starter.ws;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 配置项（前缀 {@code scaffold.ws}）。
 *
 * <p>默认关闭：SSE 覆盖绝大多数推送场景，需要双向通信（聊天、协同编辑）
 * 时再显式开启；开启后与 SSE 共用同一套 {@link com.scaffold.starter.push.PushService}
 * 与多实例转发，业务推送代码零改动。</p>
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "scaffold.ws")
public class WsProperties {

    /** starter 开关（默认关闭） */
    private boolean enabled = false;

    /** 握手端点路径 */
    private String path = "/online/ws";

    /** 允许的来源（跨域；* 表示不限制，生产建议收敛） */
    private String allowedOrigins = "*";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
