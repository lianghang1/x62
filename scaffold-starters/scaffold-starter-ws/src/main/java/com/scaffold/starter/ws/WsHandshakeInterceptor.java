package com.scaffold.starter.ws;

import com.scaffold.starter.security.token.SessionInfo;
import com.scaffold.starter.security.token.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器：复用 token 会话完成鉴权。
 *
 * <p>浏览器 WebSocket 无法自定义请求头，token 支持 query 参数
 * （{@code /online/ws?token=xxx}）与 Authorization 头两种方式；
 * 解析失败直接拒绝握手（HTTP 401 语义）。</p>
 *
 * @author lianghang
 */
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WsHandshakeInterceptor.class);

    private final TokenService tokenService;

    public WsHandshakeInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;
        if (request instanceof ServletServerHttpRequest) {
            String query = ((ServletServerHttpRequest) request).getServletRequest().getQueryString();
            if (query != null) {
                for (String pair : query.split("&")) {
                    if (pair.startsWith("token=")) {
                        token = pair.substring("token=".length());
                        break;
                    }
                }
            }
            if (token == null) {
                token = ((ServletServerHttpRequest) request).getServletRequest()
                        .getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring("Bearer ".length());
                }
            }
        }
        SessionInfo session = token == null ? null : tokenService.resolve(token);
        if (session == null) {
            log.warn("[ws] 握手拒绝：token 无效或缺失 uri={}", request.getURI());
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(ScaffoldWebSocketHandler.ATTR_ACCOUNT_ID, session.getAccountId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}
