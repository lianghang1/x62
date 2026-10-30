package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.OperLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 操作日志仓储。
 *
 * <p>写入经 {@code AuditLogListener} 异步落库；审计查询走 {@link JpaSpecificationExecutor}
 * 动态条件（模块/操作人/成败/时间段），由 AuditInternalController 消费。</p>
 *
 * @author lianghang
 */
public interface OperLogRepository extends JpaRepository<OperLog, Long>,
        JpaSpecificationExecutor<OperLog> {
}
