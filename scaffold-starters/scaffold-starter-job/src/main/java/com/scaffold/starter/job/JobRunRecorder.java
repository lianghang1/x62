package com.scaffold.starter.job;

import com.scaffold.starter.protection.lock.RedisLockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 执行日志记录器：基类通过 ThreadLocal 感知当前运行环境（仓库/锁工具），
 * 纯单元测试场景（无 Spring）自动退化为无日志模式，业务子类无感知。
 */
public final class JobRunRecorder {

    private static final Logger log = LoggerFactory.getLogger(JobRunRecorder.class);

    private static final ThreadLocal<JobRunRecorder> CURRENT = new ThreadLocal<>();

    private final JobRunLogRepository repository;
    private final RedisLockUtil lockUtil;
    private final TransactionTemplate transactionTemplate;

    private JobRunRecorder(JobRunLogRepository repository, RedisLockUtil lockUtil,
                           TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.lockUtil = lockUtil;
        this.transactionTemplate = transactionTemplate;
    }

    public static void install(JobRunLogRepository repository, RedisLockUtil lockUtil,
                               TransactionTemplate transactionTemplate) {
        CURRENT.set(new JobRunRecorder(repository, lockUtil, transactionTemplate));
    }

    static JobRunRecorder current() {
        return CURRENT.get();
    }

    public static void uninstall() {
        CURRENT.remove();
    }

    String tryLock(String key, long waitMillis, int leaseSeconds) {
        return lockUtil == null ? null : lockUtil.tryLock(key, waitMillis, leaseSeconds);
    }

    void unlock(String key, String holder) {
        if (lockUtil != null) {
            lockUtil.unlock(key, holder);
        }
    }

    Long begin(String jobName) {
        if (repository == null) {
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

    void success(Long runLogId, long costMillis) {
        finish(runLogId, "SUCCESS", costMillis, null);
    }

    void fail(Long runLogId, long costMillis, Exception error) {
        finish(runLogId, "FAIL", costMillis, error);
    }

    private void finish(Long runLogId, String status, long costMillis, Exception error) {
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
