package com.scaffold.starter.http.transport;

import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.GlobalErrorCode;

/**
 * 传输层异常：建连/超时/IO 失败的唯一表达。
 *
 * <p>携带 {@link TransportErrorType} 分类；与业务失败区分——
 * HTTP 4xx/5xx 是正常响应不抛此异常。全局异常处理器将其转为
 * 「三方服务不可用」语义的统一响应。</p>
 *
 * @author lianghang
 */
public class TransportException extends BizException {

    private static final long serialVersionUID = 1L;

    private final TransportErrorType errorType;

    public TransportException(TransportErrorType errorType, String message, Throwable cause) {
        super(GlobalErrorCode.SYSTEM_ERROR, message, cause);
        this.errorType = errorType;
    }

    public TransportException(TransportErrorType errorType, String message) {
        super(GlobalErrorCode.SYSTEM_ERROR, message);
        this.errorType = errorType;
    }

    public TransportErrorType getErrorType() {
        return errorType;
    }
}
