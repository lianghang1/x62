package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.OperLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 操作日志仓储。
 *
 * <p>日志只写不查本模块（审计查询按需走索引字段），
 * 无额外派生查询方法。</p>
 *
 * @author lianghang
 */
public interface OperLogRepository extends JpaRepository<OperLog, Long> {
}
