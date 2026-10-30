package com.scaffold.starter.test;

import com.scaffold.starter.test.annotation.ScaffoldDataSlice;
import com.scaffold.starter.test.annotation.WithAuditor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code @ScaffoldDataSlice} 切片自证测试：验证 H2 内存库 + 雪花 ID + 审计人
 * 注入一条龙可用，业务方数据层测试不再需要搭环境。
 *
 * @author lianghang
 */
@ScaffoldDataSlice
@WithAuditor("tester")
class DataSliceTest {

    /**
     * 切片需要测试类可见的 {@code @SpringBootConfiguration}：业务方即应用主类，
     * 本模块内用嵌套静态类代替。实体与 Repository 的扫描由
     * {@code DataJpaAutoConfiguration}（com.scaffold 全包）兜底，无需重复声明。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestBoot {
    }

    @Autowired
    private DemoTaskRepository repository;

    @Test
    @WithAuditor("alice")
    void snowflakeIdAndAuditFieldsFilledAutomatically() {
        DemoTask task = new DemoTask();
        task.setTitle("写周报");
        DemoTask saved = repository.saveAndFlush(task);

        // 雪花主键在应用侧生成
        assertNotNull(saved.getId());
        // 审计人来自 @WithAuditor（方法级优先于类级 tester）
        assertEquals("alice", saved.getCreateBy());
        assertNotNull(saved.getCreateTime());
        // 逻辑删除默认关闭
        assertEquals(Boolean.FALSE, saved.getDeleted());
    }

    @Test
    void classLevelAuditorAppliesWhenMethodNotAnnotated() {
        DemoTask task = new DemoTask();
        task.setTitle("代码评审");
        DemoTask saved = repository.saveAndFlush(task);
        assertEquals("tester", saved.getCreateBy());
        assertTrue(saved.getId() > 0);
    }
}
