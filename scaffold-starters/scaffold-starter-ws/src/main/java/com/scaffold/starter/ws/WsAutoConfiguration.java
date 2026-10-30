package com.scaffold.starter.ws;

import com.scaffold.starter.push.PushService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * WebSocket 自动装配入口（{@code scaffold.ws.enabled=true} 时生效）。
 *
 * <p>装配内容：会话注册表 + 主处理器 + 握手拦截器 + WsPushChannel 挂入
 * 推送体系；上下行协议与 SSE 信封一致（PushMessage JSON 文本）。</p>
 *
 * @author lianghang
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "scaffold.ws", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(WsProperties.class)
public class WsAutoConfiguration {

    @Bean
    public WsSessionRegistry wsSessionRegistry() {
        return new WsSessionRegistry();
    }

    @Bean
    public ScaffoldWebSocketHandler scaffoldWebSocketHandler(WsSessionRegistry registry,
                                                             ObjectProvider<WsMessageListener> listenerProvider) {
        return new ScaffoldWebSocketHandler(registry, listenerProvider.orderedStream()
                .collect(java.util.stream.Collectors.toList()));
    }

    /** WebSocket 通道挂入推送体系（与 SSE 通道并列，业务一次推送两通道可达）。 */
    @Bean
    public PushChannelRegistration pushChannelRegistration(WsSessionRegistry registry,
                                                           PushService pushService) {
        pushService.registerChannel(new WsPushChannel(registry));
        return new PushChannelRegistration();
    }

    /** 仅作通道注册的标记 Bean（显式表达依赖顺序）。 */
    public static final class PushChannelRegistration {
    }

    @Configuration
    @EnableWebSocket
    static class WsEndpointConfig implements WebSocketConfigurer {

        private final WsProperties properties;
        private final WsSessionRegistry registry;
        private final ScaffoldWebSocketHandler handler;
        private final com.scaffold.starter.security.token.TokenService tokenService;

        WsEndpointConfig(WsProperties properties, WsSessionRegistry registry,
                         ScaffoldWebSocketHandler handler,
                         com.scaffold.starter.security.token.TokenService tokenService) {
            this.properties = properties;
            this.registry = registry;
            this.handler = handler;
            this.tokenService = tokenService;
        }

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
            webSocketHandlerRegistry
                    .addHandler(handler, properties.getPath())
                    .addInterceptors(new WsHandshakeInterceptor(tokenService))
                    .setAllowedOrigins(properties.getAllowedOrigins().split(","));
        }
    }
}
