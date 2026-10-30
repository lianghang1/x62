package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 任务配置表（sys_job）：simple 引擎的数据驱动配置载体。
 *
 * <p>定位是「可选覆盖层」而非必需层：表里有的任务用表上的 cron/开关（支持运行时改），
 * 表里没有的回退代码注解默认值；无数据库场景（server-lite）整层缺席，纯代码驱动。
 * powerjob 引擎不消费本表，任务管理走 PowerJob Server 控制台。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_job")
public class SysJob extends BaseEntity {

    /** 任务唯一键（@Job 注解任务的 jobKey） */
    @Column(name = "job_key", length = 128, nullable = false, unique = true)
    private String jobKey;

    /** cron 表达式（覆盖代码默认值） */
    @Column(name = "cron", length = 64, nullable = false)
    private String cron;

    /** 启停开关：停用即从调度器摘除 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    /** 变更备注（谁改的、为什么改） */
    @Column(name = "remark", length = 255)
    private String remark;

    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
