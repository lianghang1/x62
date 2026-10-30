package com.scaffold.starter.push.sse;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.identity.LoginUser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 订阅端点（/online 入口语义）：持 token 会话建立事件流。
 *
 * <p>认证复用 OnlineTokenFilter——请求头带 Authorization 时 LoginContext
 * 已就绪，此处只取当前用户登记连接；未登录直接 401。前端用
 * EventSource/fetch-stream 连接后，收 {@code hb} 心跳与业务事件，
 * 断线自动清理、重连重新订阅。</p>
 *
 * @author lianghang
 */
@RestController
public class OnlineSubscribeController {

    private final SseEmitterRegistry registry;

    public OnlineSubscribeController(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping(value = "/online/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        LoginUser user = LoginContext.current();
        if (user == null) {
            throw new BizException(GlobalErrorCode.UNAUTHORIZED);
        }
        return registry.register(user.getAccountId());
    }
}
