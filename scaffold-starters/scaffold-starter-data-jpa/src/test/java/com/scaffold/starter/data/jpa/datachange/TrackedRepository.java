package com.scaffold.starter.data.jpa.datachange;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 留痕测试仓储。
 *
 * @author lianghang
 */
public interface TrackedRepository extends JpaRepository<TrackedRecord, Long> {
}
