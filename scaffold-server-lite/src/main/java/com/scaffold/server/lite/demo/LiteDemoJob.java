package com.scaffold.server.lite.demo;

import com.scaffold.starter.job.AbstractJob;
import com.scaffold.starter.job.JobContext;
import com.scaffold.starter.job.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 轻量版示例任务：演示零依赖场景下的定时任务接入方式。
 *
 * <p>继承 AbstractJob + 打 {@code @Job} 注解即完成注册，与完整版写法完全一致；
 * 差异仅在运行环境——本示例不依赖数据库与 Redis，执行日志走应用日志。
 * cron 支持配置占位符，改配置不发版即可调整频率。</p>
 *
 * @author lianghang
 */
@Component
@Job(name = "lite-demo", cron = "${job.lite-demo.cron:*/10 * * * * ?}")
public class LiteDemoJob extends AbstractJob {

    private static final Logger log = LoggerFactory.getLogger(LiteDemoJob.class);

    @Override
    protected void doExecute(JobContext context) {
        log.info("[lite-demo] 零依赖环境任务执行中 runLogId={}", context.getRunLogId());
    }
}
