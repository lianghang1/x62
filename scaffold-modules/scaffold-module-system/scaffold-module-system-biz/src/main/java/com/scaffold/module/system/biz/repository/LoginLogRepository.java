package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 登录日志仓储。
 *
 * <p>日志只写不查本模块（审计查询按需走索引字段），
 * 无额外派生查询方法。</p>
 *
 * @author lianghang
 */
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}
