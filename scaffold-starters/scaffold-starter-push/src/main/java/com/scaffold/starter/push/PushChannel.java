package com.scaffold.starter.push;

/**
 * 推送通道 SPI：SSE 与 WebSocket 是同层的两个通道实现。
 *
 * <p>{@code starter-push} 内置 SSE 通道；引入 {@code starter-ws} 后其
 * WebSocket 通道 Bean 自动加入本列表——业务面向 {@link PushService}
 * 推送，消息同时到达在线的各通道连接，业务代码不感知通道差异。</p>
 *
 * @author lianghang
 */
public interface PushChannel {

    /**
     * 投递一条消息（ accountId 为 null 表示广播本机全部连接）。
     *
     * <p>实现必须吞掉单个连接的投递异常（清理失效连接即可），
     * 不能让一个死连接阻断其他连接的投递。</p>
     *
     * @param message 消息信封
     */
    void push(PushMessage message);
}
