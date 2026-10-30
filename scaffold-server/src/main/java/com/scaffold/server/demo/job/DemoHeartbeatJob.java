package com.scaffold.server.demo.job;

import com.scaffold.starter.job.AbstractJob;
import com.scaffold.starter.job.Job;
import com.scaffold.starter.job.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 完整版示例任务：演示执行日志落库（job_run_log）与 Redis 分布式锁的完整链路。
 *
 * <p>与 lite 版写法完全一致，差异仅在运行环境——本任务在有数据库、有 Redis 的
 * 环境下运行：每次执行落一行 job_run_log，多实例部署时同任务只跑一个节点。</p>
 *
 * @author lianghang
 */
@Component
@Job(name = "demo-heartbeat", cron = "${job.demo-heartbeat.cron:*/30 * * * * ?}")
public class DemoHeartbeatJob extends AbstractJob {

    private static final Logger log = LoggerFactory.getLogger(DemoHeartbeatJob.class);

    @Override
    protected void doExecute(JobContext context) {
        log.info("[demo-heartbeat] 心跳任务执行中 runLogId={}", context.getRunLogId());
    }
}
