package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 操作日志：@OperLog 标注的方法每次调用一行（create_time 即操作时间）。
 */
@Entity
@Table(name = "sys_oper_log", indexes = {
        @Index(name = "idx_oper_log_module", columnList = "module"),
        @Index(name = "idx_oper_log_operator", columnList = "operator")
})
public class OperLog extends BaseEntity {

    @Column(name = "module", length = 64, nullable = false)
    private String module;

    @Column(name = "action", length = 64, nullable = false)
    private String action;

    @Column(name = "operator", length = 64)
    private String operator;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "uri", length = 255)
    private String uri;

    /** 入参 JSON（超长截断） */
    @Column(name = "params", length = 2048)
    private String params;

    @Column(name = "cost_millis")
    private Long costMillis;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Long getCostMillis() {
        return costMillis;
    }

    public void setCostMillis(Long costMillis) {
        this.costMillis = costMillis;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
