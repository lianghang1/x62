package com.scaffold.core.result;

import com.scaffold.core.exception.ErrorCode;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 统一响应体。所有 HTTP 接口的返回值一律使用 R 包装（文件流/导出等特殊端点除外）。
 *
 * <p>traceId 自动从 MDC 取值填充，调用方排查问题时直接把响应体里的 traceId 报给后端即可。
 *
 * @param <T> 数据类型
 */
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TRACE_ID_KEY = "traceId";

    /** 业务码：0 成功，非 0 失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 链路追踪 ID */
    private String traceId;

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get(TRACE_ID_KEY);
    }

    // ---------- 静态工厂 ----------

    public static <T> R<T> ok() {
        return new R<>(0, "成功", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(0, "成功", data);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        return new R<>(errorCode.getCode(), message, null);
    }

    // ---------- getter / setter ----------

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
