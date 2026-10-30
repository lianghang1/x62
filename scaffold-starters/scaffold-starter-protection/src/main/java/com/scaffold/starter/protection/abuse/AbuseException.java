package com.scaffold.starter.protection.abuse;

import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.GlobalErrorCode;

/**
 * 防刷阈值触发异常（HTTP 429 语义）。
 *
 * <p>由 {@link AbuseGuard} 统一抛出，携带场景与已触发次数；
 * 全局异常处理器按 {@link GlobalErrorCode#TOO_MANY_REQUESTS} 收敛为统一响应。</p>
 *
 * @author lianghang
 */
public class AbuseException extends BizException {

    private final String scene;
    private final String identity;

    /**
     * 构造防刷异常
     *
     * @param scene    防刷场景（如 sms、login）
     * @param identity 触发主体（手机号/IP/账号等）
     * @param message  面向调用方的提示文案
     */
    public AbuseException(String scene, String identity, String message) {
        super(GlobalErrorCode.TOO_MANY_REQUESTS, message);
        this.scene = scene;
        this.identity = identity;
    }

    public String getScene() {
        return scene;
    }

    public String getIdentity() {
        return identity;
    }
}
