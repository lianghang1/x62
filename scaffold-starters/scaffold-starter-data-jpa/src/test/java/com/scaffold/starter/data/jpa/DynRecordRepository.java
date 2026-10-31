package com.scaffold.starter.data.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 动态数据源测试仓储。
 *
 * @author lianghang
 */
public interface DynRecordRepository extends JpaRepository<DynRecord, Long> {
}
