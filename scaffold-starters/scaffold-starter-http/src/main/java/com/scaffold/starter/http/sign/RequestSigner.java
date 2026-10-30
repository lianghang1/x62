package com.scaffold.starter.http.sign;

import com.scaffold.starter.http.transport.TransportRequest;

/**
 * 出站请求签名 SPI：对接三方网关/自家 openapi 的协议差异。
 *
 * <p>在 transport 发送前调用，实现方按协议改写请求（追加签名头、
 * 计算时间戳与 nonce 等）。脚手架内置 HMAC 签名器见
 * {@link HmacRequestSigner}；微信支付、支付宝等私有协议在业务模块实现本接口。</p>
 *
 * @author lianghang
 */
public interface RequestSigner {

    /**
     * 对请求就地加工（返回新实例或原实例均可，最终以返回值为准）。
     *
     * @param request 未签名请求
     * @return 签名后的请求
     */
    TransportRequest sign(TransportRequest request);
}
