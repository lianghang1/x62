package com.scaffold.starter.job;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 任务执行日志：每次执行一行——payment-engine 缺失的关键能力，任务挂了不再靠翻应用日志。
 */
@Entity
@Table(name = "job_run_log")
public class JobRunLog extends BaseEntity {

    @Column(name = "job_name", length = 128, nullable = false)
    private String jobName;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    /** 耗时（毫秒） */
    @Column(name = "cost_millis")
    private Long costMillis;

    /** SUCCESS / FAIL / RUNNING */
    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getCostMillis() {
        return costMillis;
    }

    public void setCostMillis(Long costMillis) {
        this.costMillis = costMillis;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
