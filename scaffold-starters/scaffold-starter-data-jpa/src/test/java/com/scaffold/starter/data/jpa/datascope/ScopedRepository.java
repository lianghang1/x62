package com.scaffold.starter.data.jpa.datascope;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 数据权限测试仓储。
 *
 * @author lianghang
 */
public interface ScopedRepository extends JpaRepository<ScopedRecord, Long> {
}
