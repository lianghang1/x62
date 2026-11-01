package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 数据变更留痕表：标注 {@code @DataChange} 的实体每次 INSERT/UPDATE/DELETE
 * 记一行，UPDATE 携带字段级新旧值 diff（changes 为 JSON 数组：
 * {@code [{"field":"name","oldValue":"a","newValue":"b"}]}）。
 *
 * <p>与操作日志（方法级、谁调了什么）互补，本表回答「数据被改成了什么样」；
 * 两者通过 traceId 互查串成完整审计链。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_change_log", indexes = {
        @Index(name = "idx_change_log_table_record", columnList = "table_name, record_id"),
        @Index(name = "idx_change_log_operator", columnList = "operator")
})
public class ChangeLog extends BaseEntity {

    /** 业务对象名（@DataChange value） */
    @Column(name = "business_name", length = 64)
    private String businessName;

    /** 数据库表名 */
    @Column(name = "table_name", length = 128, nullable = false)
    private String tableName;

    /** 记录主键（字符串化，兼容雪花 Long） */
    @Column(name = "record_id", length = 64, nullable = false)
    private String recordId;

    /** 变更类型：INSERT / UPDATE / DELETE */
    @Column(name = "operate_type", length = 16, nullable = false)
    private String operateType;

    /** 字段变更明细 JSON（INSERT 为初值清单；UPDATE 为新旧值 diff；DELETE 为空数组） */
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;

    /** 操作人（审计上下文当前值，匿名链路为 null） */
    @Column(name = "operator", length = 64)
    private String operator;

    /** 链路标识，与访问日志/操作日志互查 */
    @Column(name = "trace_id", length = 32)
    private String traceId;

    /** 变更发生时间（事务提交时刻） */
    @Column(name = "occurred_at", nullable = false)
    private java.time.LocalDateTime occurredAt;

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getOperateType() {
        return operateType;
    }

    public void setOperateType(String operateType) {
        this.operateType = operateType;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public java.time.LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(java.time.LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
