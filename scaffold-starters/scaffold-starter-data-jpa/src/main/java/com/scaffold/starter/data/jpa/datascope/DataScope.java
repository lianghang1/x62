package com.scaffold.starter.data.jpa.datascope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解，标注在服务方法或类上，声明本次调用的数据可见范围。
 *
 * <p>SELF 模式通过 Hibernate Filter 在 SQL 层追加 {@code create_by = 当前审计人}
 * 条件，被查询实体须在类上标注 {@code @Filter(name = "scaffold_data_scope_self")}
 * 显式加入过滤——不标注的实体不受影响（白名单式，避免全量误伤）。</p>
 *
 * <p>两条行为保证：</p>
 * <ul>
 *   <li>fail-closed：SELF 模式无登录态（ScaffoldAuditorHolder 未写入）直接抛异常，
 *       绝不静默放全量；</li>
 *   <li>事务自动兜底：方法已在事务内则复用，否则切面自动开启（REQUIRED）——
 *       Filter 必须挂在贯穿全部查询的 Session 上才有效，这一步不依赖使用者的
 *       @Transactional 习惯，注解顺序也无关。</li>
 * </ul>
 *
 * <p>已知边界（Hibernate Filter 机制决定）：对 HQL / Criteria 查询生效，
 * 对 {@code em.find()} 按主键直达与原生 SQL 不生效——按 id 越权访问请另行校验。</p>
 *
 * @author lianghang
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /** 数据可见范围。 */
    DataScopeType value();
}
