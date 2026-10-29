package com.scaffold.starter.job;

import java.time.LocalDateTime;

/**
 * 任务执行上下文：本次触发信息，由基类构造后传入 doExecute。
 */
public class JobContext {

    private String jobName;

    private LocalDateTime fireTime;

    /** 本次执行的 job_run_log 记录 ID（如需回写补充信息） */
    private Long runLogId;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDateTime getFireTime() {
        return fireTime;
    }

    public void setFireTime(LocalDateTime fireTime) {
        this.fireTime = fireTime;
    }

    public Long getRunLogId() {
        return runLogId;
    }

    public void setRunLogId(Long runLogId) {
        this.runLogId = runLogId;
    }
}
