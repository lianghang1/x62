package com.scaffold.starter.http.transport;

/**
 * HTTP 传输层 SPI：把 {@link TransportRequest} 发出去并返回 {@link TransportResponse}。
 *
 * <p>实现必须是线程安全的。脚手架内置 okhttp 实现；对接特殊协议（如国密网关）时
 * 自行实现本接口注册为 Bean 即可替换。</p>
 *
 * @author lianghang
 */
public interface HttpTransport {

    /**
     * 发送请求。
     *
     * <p>约定：只抛 {@link TransportException}；HTTP 4xx/5xx 以正常响应返回。</p>
     *
     * @param request 请求快照
     * @return 响应（含非 2xx 状态码）
     */
    TransportResponse execute(TransportRequest request);
}
