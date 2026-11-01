package com.scaffold.starter.data.jpa.datachange;

import com.scaffold.starter.data.jpa.audit.ScaffoldAuditorHolder;
import com.scaffold.starter.data.jpa.entity.BaseEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据变更留痕集成测试：INSERT 记初值、UPDATE 记新旧值 diff、DELETE 记类型、
 * 未标注实体零开销、回滚不发事件。
 *
 * @author lianghang
 */
@SpringBootTest(classes = DataChangeTest.TestApp.class,
        properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class DataChangeTest {

    @Autowired
    TrackedRepository trackedRepository;
    @Autowired
    UntrackedRepository untrackedRepository;
    @Autowired
    EventCollector collector;
    @Autowired
    RollbackService rollbackService;

    @BeforeEach
    void cleanUp() {
        trackedRepository.deleteAll();
        untrackedRepository.deleteAll();
        collector.clear();
        ScaffoldAuditorHolder.set("alice");
    }

    @AfterEach
    void clearAuditor() {
        ScaffoldAuditorHolder.clear();
    }

    @Test
    void insertShouldRecordInitialValues() {
        Long id = trackedRepository.save(new TrackedRecord("张三", 20)).getId();

        List<DataChangedEvent> events = collector.events();
        assertThat(events).hasSize(1);
        DataChangedEvent event = events.get(0);
        assertThat(event.getOperateType()).isEqualTo(DataChangedEvent.OperateType.INSERT);
        assertThat(event.getBusinessName()).isEqualTo("测试对象");
        assertThat(event.getTableName()).isEqualTo("tracked_record");
        assertThat(event.getRecordId()).isEqualTo(String.valueOf(id));
        assertThat(event.getOperator()).isEqualTo("alice");
        assertThat(event.getChanges())
                .extracting(DataChangedEvent.FieldChange::getField, DataChangedEvent.FieldChange::getNewValue)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("name", "张三"),
                        org.assertj.core.groups.Tuple.tuple("age", "20"));
    }

    @Test
    void updateShouldDiffOnlyChangedFields() {
        TrackedRecord saved = trackedRepository.save(new TrackedRecord("张三", 20));
        collector.clear();

        TrackedRecord loaded = trackedRepository.findById(saved.getId()).orElseThrow();
        loaded.setName("李四");
        trackedRepository.save(loaded);

        List<DataChangedEvent> events = collector.events();
        assertThat(events).hasSize(1);
        DataChangedEvent event = events.get(0);
        assertThat(event.getOperateType()).isEqualTo(DataChangedEvent.OperateType.UPDATE);
        assertThat(event.getChanges()).hasSize(1);
        DataChangedEvent.FieldChange change = event.getChanges().get(0);
        assertThat(change.getField()).isEqualTo("name");
        assertThat(change.getOldValue()).isEqualTo("张三");
        assertThat(change.getNewValue()).isEqualTo("李四");
    }

    @Test
    void deleteShouldRecordOperateType() {
        TrackedRecord saved = trackedRepository.save(new TrackedRecord("张三", 20));
        collector.clear();

        trackedRepository.deleteById(saved.getId());

        assertThat(collector.events()).hasSize(1);
        assertThat(collector.events().get(0).getOperateType()).isEqualTo(DataChangedEvent.OperateType.DELETE);
        assertThat(collector.events().get(0).getRecordId()).isEqualTo(String.valueOf(saved.getId()));
    }

    @Test
    void entityWithoutAnnotationShouldPublishNothing() {
        untrackedRepository.save(new UntrackedRecord("无痕数据"));
        assertThat(collector.events()).isEmpty();
    }

    @Test
    void rollbackShouldNotPublish() {
        try {
            rollbackService.saveThenFail(new TrackedRecord("回滚数据", 1));
        } catch (IllegalStateException expected) {
            // 业务异常驱动事务回滚
        }
        assertThat(trackedRepository.findAll()).isEmpty();
        assertThat(collector.events()).isEmpty();
    }

    @SpringBootApplication
    static class TestApp {
    }
}
