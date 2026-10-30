package com.scaffold.starter.test.annotation;

import com.scaffold.starter.data.jpa.config.DataJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据层测试切片：只装配 JPA 相关自动配置 + scaffold-data-jpa 基础设施。
 *
 * <p>在 {@link DataJpaTest} 之上补齐框架侧数据层约定：{@code @EntityScan}/
 * {@code @EnableJpaRepositories} 扫描 {@code com.scaffold} 全包（业务模块实体
 * 无需重复声明）、雪花 ID 主键生成、JPA Auditing（审计人由
 * {@code ScaffoldAuditorHolder} 提供，配合 {@code @WithAuditor} 直接断言
 * createBy/updateBy）。测试类路径放 H2 即内存库运行，无需外部数据库。</p>
 *
 * <p>用法：{@code @ScaffoldDataSlice} 标注测试类，注入 Repository 直接测；
 * 默认每个测试方法事务回滚，不留脏数据。</p>
 *
 * @author lianghang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DataJpaTest
@ImportAutoConfiguration(DataJpaAutoConfiguration.class)
public @interface ScaffoldDataSlice {
}
