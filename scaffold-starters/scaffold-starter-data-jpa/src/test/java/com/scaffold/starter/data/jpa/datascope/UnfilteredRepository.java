package com.scaffold.starter.data.jpa.datascope;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 数据权限测试仓储（白名单外实体）。
 *
 * @author lianghang
 */
public interface UnfilteredRepository extends JpaRepository<UnfilteredRecord, Long> {
}
