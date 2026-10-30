package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.SysJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 任务配置仓储（sys_job）。
 *
 * @author lianghang
 */
public interface SysJobRepository extends JpaRepository<SysJob, Long> {

    /** 按任务唯一键查询未删除配置。 */
    Optional<SysJob> findByJobKeyAndDeletedFalse(String jobKey);
}
