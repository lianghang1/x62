package com.scaffold.common.exception;

/**
 * 业务错误码的统一契约。
 *
 * <p>错误码按业务域分段管理（详见计划文档）：0 表示成功，1xxx 为通用段（参数/认证/权限/系统），
 * 2xxx 为账户，3xxx 为订单，其余业务域在各自模块内继续分段——
 * 本接口只定义取值方式，不集中注册错误码。</p>
 *
 * @author lianghang
 */
public interface ErrorCode {

    /** 业务错误码。 */
    int getCode();

    /** 默认错误提示（面向调用方可读）。 */
    String getMessage();

    /**
     * 建议的 HTTP 状态码；返回 -1 表示使用 200（国内常用风格：HTTP 200 + 业务码表达错误）。
     * 认证/权限类错误应返回 401/403，实现「HTTP 状态码与业务码双轨一致」。
     */
    default int getHttpStatus() {
        return -1;
    }
}
