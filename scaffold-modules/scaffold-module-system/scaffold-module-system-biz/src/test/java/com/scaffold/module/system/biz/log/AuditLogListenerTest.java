package com.scaffold.module.system.biz.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.module.system.biz.entity.ChangeLog;
import com.scaffold.module.system.biz.repository.ChangeLogRepository;
import com.scaffold.module.system.biz.repository.LoginLogRepository;
import com.scaffold.module.system.biz.repository.OperLogRepository;
import com.scaffold.starter.data.jpa.datachange.DataChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 数据变更留痕落库单测：事件 → sys_change_log 字段映射与明细 JSON 化。
 *
 * @author lianghang
 */
class AuditLogListenerTest {

    private ChangeLogRepository changeLogRepository;
    private AuditLogListener listener;

    @BeforeEach
    void setUp() {
        changeLogRepository = mock(ChangeLogRepository.class);
        listener = new AuditLogListener(mock(LoginLogRepository.class), mock(OperLogRepository.class),
                changeLogRepository, new ObjectMapper());
    }

    @Test
    void changeEventShouldBeSavedAsJson() {
        DataChangedEvent event = new DataChangedEvent("账户", "sys_account", "123",
                DataChangedEvent.OperateType.UPDATE,
                Arrays.asList(new DataChangedEvent.FieldChange("name", "a", "b")),
                "alice", "trace-1", LocalDateTime.of(2026, 11, 1, 12, 0));

        listener.onDataChange(event);

        ArgumentCaptor<ChangeLog> captor = ArgumentCaptor.forClass(ChangeLog.class);
        verify(changeLogRepository).save(captor.capture());
        ChangeLog saved = captor.getValue();
        assertThat(saved.getTableName()).isEqualTo("sys_account");
        assertThat(saved.getRecordId()).isEqualTo("123");
        assertThat(saved.getOperateType()).isEqualTo("UPDATE");
        assertThat(saved.getOperator()).isEqualTo("alice");
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getChanges()).isEqualTo("[{\"field\":\"name\",\"oldValue\":\"a\",\"newValue\":\"b\"}]");
    }

    @Test
    void brokenEventShouldNeverThrow() {
        // changes 含不可序列化对象时：落库失败只告警，不向主流程抛异常
        DataChangedEvent event = new DataChangedEvent("账户", "sys_account", "1",
                DataChangedEvent.OperateType.UPDATE, null, null, null, LocalDateTime.now());

        try {
            listener.onDataChange(event);
        } catch (Exception e) {
            throw new AssertionError("落库失败不应抛异常", e);
        }
        // null changes 序列化为 "null" 也会保存（记录类型与主键仍有审计价值）
        verify(changeLogRepository).save(any(ChangeLog.class));
    }
}
