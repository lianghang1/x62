package com.scaffold.starter.ws;

/**
 * WebSocket 上行消息监听 SPI：业务实现后注册为 Bean 即可收到客户端上行文本。
 *
 * <p>协议约定与 SSE 信封对称——上下行均为 {@code PushMessage} 的 JSON 文本；
 * 需要二进制/分片等高级特性时在业务模块自行扩展 Handler。</p>
 *
 * @author lianghang
 */
public interface WsMessageListener {

    /**
     * 收到客户端上行文本。
     *
     * @param accountId 发送方账户
     * @param text      文本内容（通常是 PushMessage 的 JSON）
     */
    void onMessage(Long accountId, String text);
}
