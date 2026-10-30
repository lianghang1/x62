package com.scaffold.starter.powerjob;

import com.scaffold.starter.job.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PowerJob 执行器适配器，把 PowerJob 的调度触发桥接到脚手架的 AbstractJob 体系。
 *
 * <p>业务任务类只继承 AbstractJob 加 @Job，零改动、可同时兼容极简引擎与 PowerJob。
 * PowerJob 控制台建任务时：处理器全限定类名填 {@code com.scaffold.starter.powerjob.PowerJobJobAdapter}；
 * 任务参数填 {@code {"jobName":"对账任务"}}（与 @Job.name 一致），或直接填 @Job.name。</p>
 *
 * <p>触发后走 AbstractJob 的 final run() 模板：enabled 检查、分布式锁、执行日志、告警钩子全部保留；
 * PowerJob 侧的重试与超时按控制台配置生效。</p>
 *
 * @author lianghang
 */
public class PowerJobJobAdapter implements BasicProcessor {

    private static final Logger log = LoggerFactory.getLogger(PowerJobJobAdapter.class);

    /** 解析 {"jobName":"xxx"} 形式的任务参数 */
    private static final Pattern JOB_NAME_PATTERN =
            Pattern.compile("\"jobName\"\\s*:\\s*\"([^\"]+)\"");

    private final ApplicationContext applicationContext;

    public PowerJobJobAdapter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        String jobName = parseJobName(context.getJobParams());
        if (jobName == null || jobName.isEmpty()) {
            return new ProcessResult(false,
                    "任务参数缺少 jobName：填 {\"jobName\":\"...\"} 或直接填 @Job.name，实际参数=" + context.getJobParams());
        }
        AbstractJob job = findJob(jobName);
        if (job == null) {
            return new ProcessResult(false, "找不到 AbstractJob 任务: " + jobName);
        }
        log.info("[powerjob] 触发任务: {}（instanceId={}）",
                jobName, context.getInstanceId());
        // final run() 模板兜底：锁 + 执行日志 + 告警钩子
        job.run();
        return new ProcessResult(true, jobName + " 执行完成");
    }

    /** 参数兼容两种写法：JSON 对象或裸任务名 */
    String parseJobName(String params) {
        if (params == null) {
            return null;
        }
        String trimmed = params.trim();
        if (trimmed.startsWith("{")) {
            Matcher matcher = JOB_NAME_PATTERN.matcher(trimmed);
            return matcher.find() ? matcher.group(1) : null;
        }
        return trimmed;
    }

    /** 按 @Job.name 匹配，其次按 Bean 名称匹配 */
    AbstractJob findJob(String jobName) {
        Map<String, AbstractJob> jobs = applicationContext.getBeansOfType(AbstractJob.class);
        for (Map.Entry<String, AbstractJob> entry : jobs.entrySet()) {
            if (jobName.equals(entry.getValue().metadata().jobName)) {
                return entry.getValue();
            }
        }
        return jobs.get(jobName);
    }
}
