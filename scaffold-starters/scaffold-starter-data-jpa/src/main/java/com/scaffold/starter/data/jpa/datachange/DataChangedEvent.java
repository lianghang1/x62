package com.scaffold.starter.data.jpa.datachange;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 数据变更事件：由 Hibernate 事件监听器在事务提交后发布，
 * 应用层 {@code @EventListener}（配 {@code @Async}）异步落库。
 *
 * <p>操作人与 traceId 在监听器线程（即业务请求线程）取齐，消费方
 * 无需再还原上下文。</p>
 *
 * @author lianghang
 */
public class DataChangedEvent {

    /** 变更类型 */
    public enum OperateType {
        /** 新增 */
        INSERT,
        /** 更新（携带字段级新旧值 diff） */
        UPDATE,
        /** 删除 */
        DELETE
    }

    /** 单个字段的变更 */
    public static class FieldChange {

        private String field;
        private String oldValue;
        private String newValue;

        public FieldChange() {
        }

        public FieldChange(String field, String oldValue, String newValue) {
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getField() {
            return field;
        }

        public String getOldValue() {
            return oldValue;
        }

        public String getNewValue() {
            return newValue;
        }
    }

    /** 业务对象名（@DataChange value 或实体类简单名） */
    private String businessName;
    /** 数据库表名 */
    private String tableName;
    /** 记录主键（字符串化，兼容雪花 Long） */
    private String recordId;
    /** 变更类型 */
    private OperateType operateType;
    /** 字段变更明细（INSERT 为初值清单；UPDATE 为新旧值 diff；DELETE 可能为空） */
    private List<FieldChange> changes;
    /** 操作人（审计持有器当前值，可能为 null——匿名链路） */
    private String operator;
    /** 链路标识，可与访问日志/操作日志互查 */
    private String traceId;
    /** 变更发生时间 */
    private LocalDateTime occurredAt;

    public DataChangedEvent() {
    }

    public DataChangedEvent(String businessName, String tableName, String recordId,
                            OperateType operateType, List<FieldChange> changes,
                            String operator, String traceId, LocalDateTime occurredAt) {
        this.businessName = businessName;
        this.tableName = tableName;
        this.recordId = recordId;
        this.operateType = operateType;
        this.changes = changes == null ? Collections.emptyList() : changes;
        this.operator = operator;
        this.traceId = traceId;
        this.occurredAt = occurredAt;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getRecordId() {
        return recordId;
    }

    public OperateType getOperateType() {
        return operateType;
    }

    public List<FieldChange> getChanges() {
        return changes;
    }

    public String getOperator() {
        return operator;
    }

    public String getTraceId() {
        return traceId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
