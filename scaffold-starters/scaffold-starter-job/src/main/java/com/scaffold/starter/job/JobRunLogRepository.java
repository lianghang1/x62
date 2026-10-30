package com.scaffold.starter.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 任务执行日志仓储（job_run_log）。
 *
 * <p>分页查询供管理端「执行日志」页使用；jobName 可空（空 = 全部任务混排）。</p>
 *
 * @author lianghang
 */
public interface JobRunLogRepository extends JpaRepository<JobRunLog, Long> {

    /** 按任务名分页查询（最新在前） */
    Page<JobRunLog> findByJobNameAndDeletedFalseOrderByIdDesc(String jobName, Pageable pageable);

    /** 全部任务混排分页（最新在前） */
    Page<JobRunLog> findAllByDeletedFalseOrderByIdDesc(Pageable pageable);
}
