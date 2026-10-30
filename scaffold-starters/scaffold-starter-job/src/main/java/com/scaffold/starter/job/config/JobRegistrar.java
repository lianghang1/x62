package com.scaffold.starter.job.config;

import com.scaffold.starter.job.AbstractJob;
import com.scaffold.starter.job.JobRunLogSink;
import com.scaffold.starter.job.JobRunRecorder;
import com.scaffold.starter.protection.lock.RedisLockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import java.time.Duration;
import java.util.Map;

/**
 * 任务注册器（极简引擎 @Scheduled + Redis 锁）：
 * 启动时扫描全部 AbstractJob Bean，按 @Job.cron 注册到显式线程池
 * （Spring 默认单线程调度器是坑，必须配池）。
 *
 * <p>升级路径：接入 PowerJob 等调度平台时，把注册源替换即可，业务任务类零改动。
 */
public class JobRegistrar implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(JobRegistrar.class);

    private final ApplicationContext applicationContext;
    private final Environment environment;
    private final TaskScheduler taskScheduler;
    private final JobRunLogSink runLogSink;
    private final RedisLockUtil lockUtil;

    public JobRegistrar(ApplicationContext applicationContext, Environment environment,
                        TaskScheduler taskScheduler, JobRunLogSink runLogSink,
                        RedisLockUtil lockUtil) {
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.taskScheduler = taskScheduler;
        this.runLogSink = runLogSink;
        this.lockUtil = lockUtil;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, AbstractJob> jobs = applicationContext.getBeansOfType(AbstractJob.class);
        for (AbstractJob job : jobs.values()) {
            try {
                AbstractJob.JobMetadata metadata = job.metadata();
                // cron/enabled 支持配置占位符，改配置即生效（重启后）
                String cron = environment.resolvePlaceholders(metadata.cron);
                boolean enabled = Boolean.parseBoolean(
                        environment.resolvePlaceholders(String.valueOf(metadata.enabled)));
                if (!enabled) {
                    log.info("[job] {} 已下线（enabled=false）", metadata.jobName);
                    continue;
                }
                taskScheduler.schedule(() -> executeWithRecorder(job), new CronTrigger(cron));
                log.info("[job] 注册任务: {} cron={}", metadata.jobName, cron);
            } catch (Exception e) {
                log.error("[job] 注册失败: {}", job.getClass().getName(), e);
            }
        }
        log.info("[job] 任务注册完成，共 {} 个", jobs.size());
    }

    /**
     * 在任务执行线程上安装/卸载运行环境（Recorder 为 ThreadLocal 语义）。
     *
     * <p>安装必须发生在执行线程：调度发生在 scheduler 线程池，
     * 若在主线程 install，执行线程里 current() 恒为 null，锁与执行日志全部失效。</p>
     */
    private void executeWithRecorder(AbstractJob job) {
        JobRunRecorder.install(runLogSink, lockUtil);
        try {
            job.run();
        } finally {
            JobRunRecorder.uninstall();
        }
    }

    /** 显式线程池：默认 4 线程，任务多时经 scaffold.job.pool-size 调整 */
    public static ThreadPoolTaskScheduler buildScheduler(int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("scaffold-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setErrorHandler(t -> LoggerFactory.getLogger("scaffold-job-error")
                .error("[job] 调度器捕获未处理异常", t));
        return scheduler;
    }

    public static TaskScheduler initScheduler(int poolSize) {
        ThreadPoolTaskScheduler scheduler = buildScheduler(poolSize);
        scheduler.initialize();
        return scheduler;
    }

    @SuppressWarnings("unused")
    private static Duration unused() {
        return Duration.ZERO;
    }
}
