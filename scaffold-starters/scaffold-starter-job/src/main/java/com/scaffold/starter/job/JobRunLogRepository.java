package com.scaffold.starter.job;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 任务执行日志仓储（job_run_log）。
 */
public interface JobRunLogRepository extends JpaRepository<JobRunLog, Long> {
}
