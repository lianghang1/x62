package com.scaffold.starter.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 执行日志的 JPA 落库实现，仅在类路径存在 JPA 时由条件装配创建。
 *
 * <p>本类是 starter 中唯一触碰 JPA 类型的执行链路组件：无数据库场景下
 * 条件不成立、本类不被加载，调度链路经 {@link JobRunLogSink} 抽象自动退化为纯应用日志。</p>
 *
 * @author lianghang
 */
public class JpaJobRunLogSink implements JobRunLogSink {

    private static final Logger log = LoggerFactory.getLogger(JpaJobRunLogSink.class);

    private final JobRunLogRepository repository;
    private final TransactionTemplate transactionTemplate;

    public JpaJobRunLogSink(JobRunLogRepository repository, TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Long begin(String jobName) {
        if (repository == null) {
            log.warn("[job] 执行日志仓储未装配（无数据库或未建表），本次运行不落执行日志");
            return null;
        }
        try {
            JobRunLog runLog = new JobRunLog();
            runLog.setJobName(jobName);
            runLog.setStartTime(java.time.LocalDateTime.now());
            runLog.setStatus("RUNNING");
            return repository.save(runLog).getId();
        } catch (Exception e) {
            log.warn("[job] 执行日志写入失败（不影响任务执行）: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void finish(Long runLogId, String status, long costMillis, Exception error) {
        if (runLogId == null || repository == null) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(txStatus -> {
                JobRunLog runLog = repository.findById(runLogId).orElse(null);
                if (runLog == null) {
                    return;
                }
                runLog.setEndTime(java.time.LocalDateTime.now());
                runLog.setCostMillis(costMillis);
                runLog.setStatus(status);
                if (error != null) {
                    String message = error.toString();
                    runLog.setErrorMessage(message.length() > 2000 ? message.substring(0, 2000) : message);
                }
                repository.save(runLog);
            });
        } catch (Exception e) {
            log.warn("[job] 执行日志收尾失败（不影响任务执行）: {}", e.getMessage());
        }
    }
}
