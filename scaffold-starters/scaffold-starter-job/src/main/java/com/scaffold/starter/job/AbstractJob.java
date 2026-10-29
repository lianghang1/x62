package com.scaffold.starter.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务模板基类：子类只写业务逻辑（doExecute），其余全部由基类包装——
 * enabled 检查 -> 分布式锁（多实例只跑一个节点）-> 计时 -> 执行日志 -> 失败告警钩子。
 * final run() 子类不可绕过。
 */
public abstract class AbstractJob {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** 模板方法：由 JobRegistrar 调度，子类禁止覆写 */
    public final void run() {
        JobMetadata metadata = metadata();
        if (!isEnabled(metadata)) {
            log.debug("[job] {} 已通过配置下线，跳过", metadata.jobName);
            return;
        }
        JobRunRecorder recorder = JobRunRecorder.current();
        long start = System.currentTimeMillis();
        JobContext ctx = new JobContext();
        ctx.setJobName(metadata.jobName);
        ctx.setFireTime(java.time.LocalDateTime.now());
        String lockHolder = null;
        try {
            if (metadata.lockLeaseSeconds > 0 && recorder != null) {
                lockHolder = recorder.tryLock("job:" + metadata.jobKey, 0, metadata.lockLeaseSeconds);
                if (lockHolder == null) {
                    log.info("[job] {} 其他节点执行中，本节点跳过", metadata.jobName);
                    return;
                }
            }
            Long runLogId = recorder == null ? null : recorder.begin(metadata.jobName);
            ctx.setRunLogId(runLogId);
            beforeExecute(ctx);
            doExecute(ctx);
            afterExecute(ctx);
            if (recorder != null) {
                recorder.success(runLogId, System.currentTimeMillis() - start);
            }
            log.info("[job] {} 执行完成，耗时 {}ms", metadata.jobName, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[job] {} 执行失败，耗时 {}ms", metadata.jobName,
                    System.currentTimeMillis() - start, e);
            if (recorder != null) {
                recorder.fail(ctx.getRunLogId(), System.currentTimeMillis() - start, e);
            }
            try {
                onError(ctx, e);
            } catch (Exception alertError) {
                log.error("[job] {} 告警钩子执行失败", metadata.jobName, alertError);
            }
        } finally {
            if (lockHolder != null && recorder != null) {
                recorder.unlock("job:" + metadata.jobKey, lockHolder);
            }
        }
    }

    /** 子类唯一必须实现：业务逻辑 */
    protected abstract void doExecute(JobContext context);

    // ---------- 可覆写钩子 ----------

    protected void beforeExecute(JobContext context) {
    }

    protected void afterExecute(JobContext context) {
    }

    /** 失败告警钩子：默认仅日志；需要邮件/短信时覆写并调用 NotificationService */
    protected void onError(JobContext context, Exception e) {
    }

    /** enabled 解析（支持 ${} 配置占位，由 registrar 在注册时注入解析器） */
    protected boolean isEnabled(JobMetadata metadata) {
        return metadata.enabled;
    }

    public final JobMetadata metadata() {
        Job annotation = getClass().getAnnotation(Job.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    getClass().getName() + " 继承 AbstractJob 必须标注 @Job 注解");
        }
        return new JobMetadata(annotation);
    }

    /** 任务元数据快照 */
    public static class JobMetadata {
        public final String jobName;
        public final String cron;
        public final boolean enabled;
        public final int lockLeaseSeconds;
        public final String jobKey;

        JobMetadata(Job annotation) {
            this.jobName = annotation.name();
            this.cron = annotation.cron();
            this.enabled = !"false".equalsIgnoreCase(annotation.enabled());
            this.lockLeaseSeconds = annotation.lockLeaseSeconds();
            this.jobKey = annotation.name().replaceAll("[^a-zA-Z0-9]", "-").toLowerCase();
        }
    }
}
