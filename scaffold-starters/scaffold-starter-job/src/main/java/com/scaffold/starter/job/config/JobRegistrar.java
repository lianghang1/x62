package com.scaffold.starter.job.config;

import com.scaffold.starter.job.AbstractJob;
import com.scaffold.starter.job.JobConfigProvider;
import com.scaffold.starter.job.JobRunLogSink;
import com.scaffold.starter.job.JobRunRecorder;
import com.scaffold.starter.protection.lock.RedisLockUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 任务注册器（极简引擎）：扫描 AbstractJob Bean 注册到显式线程池，
 * 支持「表配置覆盖代码默认值 + 运行时改 cron/启停 + 立即触发」。
 *
 * <p>回退链：外部配置（{@link JobConfigProvider}，如 sys_job 表）&gt; 代码注解默认值；
 * 无提供方（server-lite 零依赖形态）自动回退纯代码驱动。</p>
 *
 * <p>接入 PowerJob 时 {@code scaffold.job.engine=none}，注册源替换为 PowerJob
 * Server 配置，业务任务类零改动。</p>
 *
 * @author lianghang
 */
public class JobRegistrar implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(JobRegistrar.class);

    private final ApplicationContext applicationContext;
    private final Environment environment;
    private final TaskScheduler taskScheduler;
    private final JobRunLogSink runLogSink;
    private final RedisLockUtil lockUtil;
    private final JobConfigProvider configProvider;
    /** 指标注册表（optional：observability 不在场时为 null，埋点静默） */
    private final MeterRegistry meterRegistry;

    /** 运行中任务的调度句柄（jobKey → future），运行时刷新按 key 取消重排 */
    private final Map<String, ScheduledFuture<?>> scheduled = new ConcurrentHashMap<>();
    private final Map<String, AbstractJob> jobsByJobKey = new ConcurrentHashMap<>();
    private final Map<String, String> codeCrons = new ConcurrentHashMap<>();

    public JobRegistrar(ApplicationContext applicationContext, Environment environment,
                        TaskScheduler taskScheduler, JobRunLogSink runLogSink,
                        RedisLockUtil lockUtil, JobConfigProvider configProvider) {
        this(applicationContext, environment, taskScheduler, runLogSink, lockUtil, configProvider, null);
    }

    public JobRegistrar(ApplicationContext applicationContext, Environment environment,
                        TaskScheduler taskScheduler, JobRunLogSink runLogSink,
                        RedisLockUtil lockUtil, JobConfigProvider configProvider,
                        MeterRegistry meterRegistry) {
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.taskScheduler = taskScheduler;
        this.runLogSink = runLogSink;
        this.lockUtil = lockUtil;
        this.configProvider = configProvider;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, AbstractJob> jobs = applicationContext.getBeansOfType(AbstractJob.class);
        int registered = 0;
        for (AbstractJob job : jobs.values()) {
            try {
                AbstractJob.JobMetadata metadata = job.metadata();
                String jobKey = metadata.jobKey;
                // 代码默认值（占位符经 Environment 解析）
                String cron = environment.resolvePlaceholders(metadata.cron);
                boolean enabled = Boolean.parseBoolean(
                        environment.resolvePlaceholders(String.valueOf(metadata.enabled)));
                codeCrons.put(jobKey, cron);
                jobsByJobKey.put(jobKey, job);
                // 外部配置覆盖（表配置 > 代码默认）
                String source = "code";
                if (configProvider != null) {
                    Optional<JobConfigProvider.JobOverride> override = configProvider.find(jobKey);
                    if (override.isPresent()) {
                        cron = environment.resolvePlaceholders(override.get().getCron());
                        enabled = override.get().isEnabled();
                        source = "db";
                    }
                }
                if (!enabled) {
                    log.info("[job] {} 已下线（enabled=false，来源 {}）", metadata.jobName, source);
                    continue;
                }
                schedule(job, jobKey, cron);
                registered++;
                log.info("[job] 注册任务: {} cron={} (来源: {})", metadata.jobName, cron, source);
            } catch (Exception e) {
                log.error("[job] 注册失败: {}", job.getClass().getName(), e);
            }
        }
        log.info("[job] 任务注册完成，共 {} 个（发现 {} 个）", registered, jobs.size());
    }

    /**
     * 运行时应用新配置（改 cron / 启停），立即生效不重启。
     *
     * @return false 表示任务不在本实例注册表（未注册或外部引擎接管），调用方不应落表
     */
    public boolean apply(String jobKey, String cron, boolean enabled) {
        AbstractJob job = jobsByJobKey.get(jobKey);
        if (job == null) {
            return false;
        }
        ScheduledFuture<?> old = scheduled.remove(jobKey);
        if (old != null) {
            old.cancel(false);
        }
        if (enabled) {
            schedule(job, jobKey, cron);
        }
        log.info("[job] 配置已应用: {} cron={} enabled={}", jobKey, cron, enabled);
        return true;
    }

    /** 立即触发一次（异步，与定时执行走同一包装，锁/日志语义一致） */
    public boolean triggerNow(String jobKey) {
        AbstractJob job = jobsByJobKey.get(jobKey);
        if (job == null) {
            return false;
        }
        taskScheduler.schedule(() -> executeWithRecorder(job), java.time.Instant.now());
        return true;
    }

    /** 注册表快照：管理端列表合并视图用（enabled = 是否在调度中） */
    public List<JobInfo> views() {
        List<JobInfo> list = new ArrayList<>();
        for (Map.Entry<String, AbstractJob> entry : jobsByJobKey.entrySet()) {
            AbstractJob.JobMetadata metadata = entry.getValue().metadata();
            list.add(new JobInfo(entry.getKey(), metadata.jobName,
                    entry.getValue().getClass().getName(),
                    codeCrons.get(entry.getKey()), scheduled.containsKey(entry.getKey())));
        }
        return list;
    }

    /**
     * 在任务执行线程上安装/卸载运行环境（Recorder 为 TTL 语义，子任务经 ScaffoldExecutors 包装可透传）。
     *
     * <p>安装必须发生在执行线程：调度发生在 scheduler 线程池，
     * 若在主线程 install，执行线程里 current() 恒为 null，锁与执行日志全部失效。</p>
     */
    private void executeWithRecorder(AbstractJob job) {
        JobRunRecorder.install(runLogSink, lockUtil);
        long start = System.currentTimeMillis();
        try {
            job.run();
            recordMetrics(job, "success", System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            recordMetrics(job, "fail", System.currentTimeMillis() - start);
            throw e;
        } finally {
            JobRunRecorder.uninstall();
        }
    }

    /** 执行指标：scaffold.job.executions（outcome 维度）+ scaffold.job.duration；无 registry 时静默。 */
    private void recordMetrics(AbstractJob job, String outcome, long costMillis) {
        if (meterRegistry == null) {
            return;
        }
        String jobKey = job.metadata().jobKey;
        meterRegistry.counter(com.scaffold.common.constant.MetricsNames.JOB_EXECUTIONS, "job", jobKey, "outcome", outcome).increment();
        Timer.builder(com.scaffold.common.constant.MetricsNames.JOB_DURATION).tag("job", jobKey).register(meterRegistry)
                .record(costMillis, TimeUnit.MILLISECONDS);
    }

    private void schedule(AbstractJob job, String jobKey, String cron) {
        ScheduledFuture<?> future = taskScheduler.schedule(() -> executeWithRecorder(job), new CronTrigger(cron));
        scheduled.put(jobKey, future);
    }

    /** 注册表条目视图：codeCron 为代码注解默认值，enabled 为当前是否在调度中 */
    public static class JobInfo {

        private final String jobKey;
        private final String jobName;
        private final String className;
        private final String codeCron;
        private final boolean enabled;

        public JobInfo(String jobKey, String jobName, String className,
                       String codeCron, boolean enabled) {
            this.jobKey = jobKey;
            this.jobName = jobName;
            this.className = className;
            this.codeCron = codeCron;
            this.enabled = enabled;
        }

        public String getJobKey() {
            return jobKey;
        }

        public String getJobName() {
            return jobName;
        }

        public String getClassName() {
            return className;
        }

        public String getCodeCron() {
            return codeCron;
        }

        public boolean isEnabled() {
            return enabled;
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
}
