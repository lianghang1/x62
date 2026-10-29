package com.scaffold.core.exception;

/**
 * 业务错误码接口。
 *
 * <p>错误码分段约定（详见计划文档）：
 * <ul>
 *   <li>0        成功</li>
 *   <li>1xxx     通用（参数/认证/权限/系统）</li>
 *   <li>2xxx     账户</li>
 *   <li>3xxx     订单</li>
 *   <li>...      各业务域继续分段</li>
 * </ul>
 */
public interface ErrorCode {

    /** 业务错误码 */
    int getCode();

    /** 默认错误提示（面向调用方可读） */
    String getMessage();

    /**
     * 建议的 HTTP 状态码；返回 -1 表示使用 200（国内常用风格：HTTP 200 + 业务码表达错误）。
     * 认证/权限类错误应返回 401/403，实现「HTTP 状态码与业务码双轨一致」。
     */
    default int getHttpStatus() {
        return -1;
    }
}
