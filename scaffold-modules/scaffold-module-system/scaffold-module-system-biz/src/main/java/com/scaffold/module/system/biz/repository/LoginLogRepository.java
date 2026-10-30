package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 登录日志仓储。
 *
 * <p>写入经 {@code AuditLogListener} 异步落库；审计查询走 {@link JpaSpecificationExecutor}
 * 动态条件（账号/类型/成败/时间段），由 AuditInternalController 消费。</p>
 *
 * @author lianghang
 */
public interface LoginLogRepository extends JpaRepository<LoginLog, Long>,
        JpaSpecificationExecutor<LoginLog> {
}
