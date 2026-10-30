package com.scaffold.starter.http.transport;

/**
 * 传输层失败分类，调用方据此决定是否重试/告警/降级。
 *
 * <p>{@code RETRYABLE_IO} 涵盖连接被重置、读中断等网络抖动类失败，
 * 配合幂等方法重试使用；协议层 4xx/5xx 不是 transport 错误，
 * 以正常 {@link TransportResponse} 返回。</p>
 *
 * @author lianghang
 */
public enum TransportErrorType {

    /** 建连失败（DNS/拒绝/不可达），通常短时间内重试大概率同样失败 */
    CONNECT,

    /** 读写超时，幂等请求可重试 */
    TIMEOUT,

    /** 网络 IO 异常（连接重置/断开），幂等请求可重试 */
    RETRYABLE_IO,

    /** 请求线程被中断，直接上抛不再重试 */
    INTERRUPTED
}
