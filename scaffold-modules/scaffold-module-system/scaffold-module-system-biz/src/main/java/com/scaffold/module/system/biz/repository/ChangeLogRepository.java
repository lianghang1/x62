package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.ChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 数据变更留痕仓储：按表+记录主键回溯单条数据的完整变更历史，
 * 按操作人追溯个人改动。
 *
 * @author lianghang
 */
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {

    /** 按表+记录主键回溯变更历史（时间倒序） */
    Page<ChangeLog> findByTableNameAndRecordIdOrderByIdDesc(String tableName, String recordId, Pageable pageable);

    /** 按操作人追溯改动（时间倒序） */
    Page<ChangeLog> findByOperatorOrderByIdDesc(String operator, Pageable pageable);
}
