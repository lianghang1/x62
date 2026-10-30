package com.scaffold.starter.push.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.Serializable;
import java.util.UUID;

/**
 * SSE 连接持有者：emitter 与其所属账户、连接标识的绑定关系。
 *
 * <p>连接断开（完成/超时/异常）时由 Spring 回调自动从注册表摘除，
 * 心跳与业务投递都通过本类访问 emitter。</p>
 *
 * @author lianghang
 */
class SseConnection implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString().replace("-", "");
    private final Long accountId;
    private final SseEmitter emitter;

    SseConnection(Long accountId, SseEmitter emitter) {
        this.accountId = accountId;
        this.emitter = emitter;
    }

    String getId() {
        return id;
    }

    Long getAccountId() {
        return accountId;
    }

    SseEmitter getEmitter() {
        return emitter;
    }
}
