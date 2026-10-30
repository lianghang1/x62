package com.scaffold.core.exception;

/**
 * 业务异常，业务代码中表达「可预期失败」的唯一方式。
 *
 * <p>只用于可预期的业务失败——不可预期的技术异常直接抛原始异常，
 * 不要包装成 BizException 掩盖堆栈；全局异常处理器会记录 warn 日志并转换为统一响应，
 * 业务代码无需自行 try/catch 后拼响应。</p>
 *
 * @author lianghang
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 使用自定义 message 覆盖错误码默认文案。 */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** 快捷抛出。 */
    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }

    /** 快捷抛出（自定义文案）。 */
    public static BizException of(ErrorCode errorCode, String message) {
        return new BizException(errorCode, message);
    }
}
