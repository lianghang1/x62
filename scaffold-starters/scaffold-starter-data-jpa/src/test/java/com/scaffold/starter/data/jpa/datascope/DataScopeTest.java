package com.scaffold.starter.data.jpa.datascope;

import com.scaffold.starter.data.jpa.audit.ScaffoldAuditorHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 数据权限集成测试：SELF 过滤按 create_by 生效、白名单外实体不受影响、
 * 无登录态与无事务均 fail-closed。
 *
 * @author lianghang
 */
@SpringBootTest(classes = DataScopeTest.TestApp.class,
        properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class DataScopeTest {

    @Autowired
    ScopedRepository scopedRepository;

    @Autowired
    UnfilteredRepository unfilteredRepository;

    @Autowired
    ScopedService scopedService;

    @AfterEach
    void clearAuditor() {
        ScaffoldAuditorHolder.clear();
    }

    @BeforeEach
    void cleanUp() {
        // Spring 上下文跨测试缓存，先清库保证各用例计数从零开始
        scopedRepository.deleteAll();
        unfilteredRepository.deleteAll();
    }

    @Test
    @Transactional
    void selfScopeShouldFilterByCreator() {
        seed("alice");
        seed("bob");

        ScaffoldAuditorHolder.set("alice");
        List<ScopedRecord> visible = scopedService.listSelf();

        assertThat(visible).hasSize(1)
                .allSatisfy(r -> assertThat(r.getCreateBy()).isEqualTo("alice"));

        ScaffoldAuditorHolder.set("bob");
        assertThat(scopedService.listSelf()).hasSize(1)
                .allSatisfy(r -> assertThat(r.getCreateBy()).isEqualTo("bob"));
    }

    @Test
    @Transactional
    void allScopeShouldSeeEverything() {
        seed("alice");
        seed("bob");

        ScaffoldAuditorHolder.set("alice");
        assertThat(scopedService.listAll()).hasSize(2);
    }

    @Test
    @Transactional
    void entitiesWithoutFilterAnnotationShouldStayVisible() {
        seed("alice");
        seed("bob");
        ScaffoldAuditorHolder.set("alice");
        unfilteredRepository.save(new UnfilteredRecord("公开数据-1"));
        unfilteredRepository.save(new UnfilteredRecord("公开数据-2"));
        unfilteredRepository.flush();

        // SELF 只过滤白名单实体；白名单外的 UnfilteredRecord 完全不受影响
        assertThat(scopedService.listSelf()).hasSize(1)
                .allSatisfy(r -> assertThat(r.getCreateBy()).isEqualTo("alice"));
        assertThat(scopedService.listUnfiltered()).hasSize(2);
    }

    @Test
    void selfScopeWithoutLoginShouldFailClosed() {
        // 故意不写入 ScaffoldAuditorHolder
        assertThatThrownBy(() -> scopedService.listSelfInTx())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("登录态");
    }

    @Test
    void selfScopeWithoutOuterTransactionShouldAutoStartTx() {
        seed("alice");
        seed("bob");
        ScaffoldAuditorHolder.set("alice");
        // 方法与调用方都没有 @Transactional：切面自动开事务，Filter 照样贯穿全部查询
        List<ScopedRecord> visible = scopedService.listSelfWithoutTx();
        assertThat(visible).hasSize(1)
                .allSatisfy(r -> assertThat(r.getCreateBy()).isEqualTo("alice"));
    }

    private void seed(String creator) {
        ScaffoldAuditorHolder.set(creator);
        scopedRepository.save(new ScopedRecord("数据-of-" + creator));
        scopedRepository.flush();
    }

    @SpringBootApplication
    static class TestApp {

        @Bean
        ScopedService scopedService() {
            return new ScopedService();
        }
    }

    @Service
    static class ScopedService {

        @Autowired
        ScopedRepository scopedRepository;

        @Autowired
        UnfilteredRepository unfilteredRepository;

        @DataScope(DataScopeType.SELF)
        @Transactional
        public List<ScopedRecord> listSelf() {
            return scopedRepository.findAll();
        }

        @DataScope(DataScopeType.ALL)
        @Transactional
        public List<ScopedRecord> listAll() {
            return scopedRepository.findAll();
        }

        @DataScope(DataScopeType.SELF)
        public List<ScopedRecord> listSelfWithoutTx() {
            return scopedRepository.findAll();
        }

        @Transactional
        public List<UnfilteredRecord> listUnfiltered() {
            return unfilteredRepository.findAll();
        }

        /** 供无登录态测试调用：内部带事务，保证失败点落在登录态校验。 */
        @DataScope(DataScopeType.SELF)
        @Transactional
        public List<ScopedRecord> listSelfInTx() {
            return scopedRepository.findAll();
        }
    }
}
