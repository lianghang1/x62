package com.scaffold.core.exception;

/**
 * 全局通用错误码枚举（1xxx 段）。
 *
 * <p>只承载跨模块通用的错误语义；各业务域的错误码在各自模块内定义，不得写入本枚举，
 * 以免业务语义向 core 扩散。枚举项同时携带默认文案与建议 HTTP 状态码，
 * 供 {@link BizException} 与统一响应直接取用。</p>
 *
 * @author lianghang
 */
public enum GlobalErrorCode implements ErrorCode {

    SUCCESS(0, "成功"),

    // ---- 通用 1xxx ----
    BAD_REQUEST(1000, "请求参数错误", 400),
    UNAUTHORIZED(1001, "未登录或凭证无效", 401),
    TOKEN_EXPIRED(1002, "凭证已过期，请重新登录", 401),
    FORBIDDEN(1003, "无访问权限", 403),
    NOT_FOUND(1404, "资源不存在", 404),
    METHOD_NOT_ALLOWED(1405, "请求方法不支持", 405),
    CONFLICT(1409, "请求冲突或重复提交", 409),
    TOO_MANY_REQUESTS(1429, "请求过于频繁，请稍后再试", 429),

    SYSTEM_ERROR(1500, "系统繁忙，请稍后再试", 500),
    SERVICE_UNAVAILABLE(1501, "服务暂不可用", 503);

    private final int code;
    private final String message;
    private final int httpStatus;

    GlobalErrorCode(int code, String message) {
        this(code, message, -1);
    }

    GlobalErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}
