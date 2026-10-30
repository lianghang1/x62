package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.OperLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperLogRepository extends JpaRepository<OperLog, Long> {
}
