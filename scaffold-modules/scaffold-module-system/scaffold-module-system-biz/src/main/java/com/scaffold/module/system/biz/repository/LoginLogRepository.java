package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}
