package com.scaffold.module.system.biz.event;

import java.time.LocalDateTime;

/**
 * 操作日志事件。
 *
 * <p>由 {@code OperLogAspect} 在方法执行后经 {@code EventOps.publishAfterCommit}
 * 发布——业务方法有事务时提交才发，回滚不发；{@code AuditLogListener} 异步落库。
 * 事件字段与 {@code sys_oper_log} 列一一对应。</p>
 *
 * @author lianghang
 */
public class OperLogEvent {

    private final String module;
    private final String action;
    private final String operator;
    private final String ip;
    private final String uri;
    private final String params;
    private final Long costMillis;
    private final boolean success;
    private final String errorMessage;
    private final LocalDateTime operTime;

    public OperLogEvent(String module, String action, String operator, String ip, String uri,
                        String params, Long costMillis, boolean success, String errorMessage,
                        LocalDateTime operTime) {
        this.module = module;
        this.action = action;
        this.operator = operator;
        this.ip = ip;
        this.uri = uri;
        this.params = params;
        this.costMillis = costMillis;
        this.success = success;
        this.errorMessage = errorMessage;
        this.operTime = operTime;
    }

    public String getModule() {
        return module;
    }

    public String getAction() {
        return action;
    }

    public String getOperator() {
        return operator;
    }

    public String getIp() {
        return ip;
    }

    public String getUri() {
        return uri;
    }

    public String getParams() {
        return params;
    }

    public Long getCostMillis() {
        return costMillis;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getOperTime() {
        return operTime;
    }
}
