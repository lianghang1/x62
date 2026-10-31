package com.scaffold.starter.data.jpa.datascope;

import com.scaffold.starter.data.jpa.audit.ScaffoldAuditorHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Session;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * {@link DataScope} 切面：SELF 模式在当前 Session 上启用 Hibernate Filter，
 * 查询自动追加 {@code create_by = 当前审计人}；方法退出后关闭 Filter。
 *
 * <p><b>事务时序</b>：Filter 挂在 Session 上，而无事务时每次查询各取独立 Session、
 * Filter 静默失效（表现为「看似过滤实则越权」）。因此本切面不依赖与事务拦截器的
 * order 竞争：方法已在事务内则直接复用，否则用 {@link TransactionTemplate}
 * （REQUIRED）自动开启——两种拦截顺序下语义一致，使用者无需关心注解顺序。</p>
 *
 * <p>fail-closed：SELF 模式且无登录态（ScaffoldAuditorHolder 未写入）直接抛
 * {@link IllegalStateException}——数据权限的失效形态必须是「报错」，不是「放全量」。</p>
 *
 * <p>已知边界（Hibernate Filter 机制决定）：对 HQL / Criteria 查询生效，
 * 对 {@code em.find()} 按主键直达与原生 SQL 不生效——按 id 越权访问请另行校验。</p>
 *
 * @author lianghang
 */
@Aspect
public class DataScopeAspect {

    /** 与 BaseEntity 上 @FilterDef 同名；实体以 @Filter(name=...) 白名单式加入。 */
    public static final String FILTER_SELF = "scaffold_data_scope_self";

    @PersistenceContext
    private EntityManager entityManager;

    private final TransactionTemplate transactionTemplate;

    public DataScopeAspect(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Pointcut("@within(com.scaffold.starter.data.jpa.datascope.DataScope)"
            + " || @annotation(com.scaffold.starter.data.jpa.datascope.DataScope)")
    public void dataScopePointcut() {
    }

    @Around("dataScopePointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        DataScope scope = resolve(point);
        if (scope == null || scope.value() == DataScopeType.ALL) {
            return point.proceed();
        }
        String owner = ScaffoldAuditorHolder.get();
        if (owner == null || owner.isEmpty()) {
            throw new IllegalStateException(
                    "@DataScope(SELF) 需要登录态（ScaffoldAuditorHolder 未写入），拒绝执行以防越权");
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return enableFilterAndProceed(point, owner);
        }
        // 无事务：自动开一个（REQUIRED），保证 Filter 挂在贯穿全部查询的 Session 上
        return transactionTemplate.execute(status -> {
            try {
                return enableFilterAndProceed(point, owner);
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        });
    }

    private Object enableFilterAndProceed(ProceedingJoinPoint point, String owner) throws Throwable {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter(FILTER_SELF).setParameter("owner", owner);
        try {
            return point.proceed();
        } finally {
            session.disableFilter(FILTER_SELF);
        }
    }

    /**
     * 方法标注优先，其次目标类标注。
     */
    private DataScope resolve(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataScope scope = signature.getMethod().getAnnotation(DataScope.class);
        if (scope == null) {
            scope = point.getTarget().getClass().getAnnotation(DataScope.class);
        }
        return scope;
    }
}
