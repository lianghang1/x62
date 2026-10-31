package com.scaffold.module.system.biz.service;

import com.scaffold.common.util.StringUtils;
import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.common.result.PageResult;
import com.scaffold.module.system.biz.entity.SysJob;
import com.scaffold.module.system.biz.repository.SysJobRepository;
import com.scaffold.starter.job.JobRunLog;
import com.scaffold.starter.job.JobRunLogRepository;
import com.scaffold.starter.job.config.JobRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务管理服务：simple 引擎的运行时管理面（列表/改配置/触发/执行日志）。
 *
 * <p>职责边界：本服务只管理 simple 引擎（JobRegistrar）；powerjob 引擎
 * （engine=none，Registrar 不装配）运行时操作不可用，任务管理走 PowerJob
 * Server 控制台，执行日志查询两个引擎通用（JobRunRecorder 统一落库）。</p>
 *
 * <p>变更流程：先落 sys_job 表（下次启动的配置源），再调 {@code registrar.apply}
 * 动态刷新调度（不重启立即生效）；两步任一失败以抛出回滚，避免表与调度器漂移。</p>
 *
 * @author lianghang
 */
@Service
public class JobAdminService {

    private static final Logger log = LoggerFactory.getLogger(JobAdminService.class);

    private final ObjectProvider<JobRegistrar> registrarProvider;
    private final SysJobRepository sysJobRepository;
    private final JobRunLogRepository runLogRepository;

    public JobAdminService(ObjectProvider<JobRegistrar> registrarProvider,
                           SysJobRepository sysJobRepository,
                           JobRunLogRepository runLogRepository) {
        this.registrarProvider = registrarProvider;
        this.sysJobRepository = sysJobRepository;
        this.runLogRepository = runLogRepository;
    }

    /** 任务列表：注册表快照（代码默认）与表配置合并视图，含仅落表未注册的孤儿配置 */
    public List<Map<String, Object>> list() {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        JobRegistrar registrar = registrarProvider.getIfAvailable();
        if (registrar != null) {
            for (JobRegistrar.JobInfo info : registrar.views()) {
                Map<String, Object> view = base(info.getJobKey(), info.getJobName(), info.getClassName());
                view.put("codeCron", info.getCodeCron());
                view.put("enabled", info.isEnabled());
                view.put("registered", true);
                merged.put(info.getJobKey(), view);
            }
        }
        for (SysJob sysJob : sysJobRepository.findAll()) {
            if (Boolean.TRUE.equals(sysJob.getDeleted())) {
                continue;
            }
            Map<String, Object> view = merged.get(sysJob.getJobKey());
            if (view == null) {
                // 孤儿配置：表里有但代码里没有（任务类已下线或 powerjob 引擎）
                view = base(sysJob.getJobKey(), sysJob.getJobKey(), null);
                view.put("codeCron", null);
                view.put("registered", false);
                merged.put(sysJob.getJobKey(), view);
            }
            view.put("configCron", sysJob.getCron());
            view.put("configEnabled", sysJob.getEnabled());
            view.put("remark", sysJob.getRemark());
            view.put("configured", true);
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 保存配置并动态刷新调度。
     *
     * <p>cron 合法性先校验（CronExpression 解析不过直接拒），防止脏配置把任务打死。</p>
     */
    @Transactional
    public void saveConfig(String jobKey, String cron, Boolean enabled, String remark) {
        if (!CronExpression.isValidExpression(cron)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "cron 表达式不合法: " + cron);
        }
        JobRegistrar registrar = registrarProvider.getIfAvailable();
        boolean registered = registrar != null && registrar.apply(jobKey, cron, Boolean.TRUE.equals(enabled));
        if (!registered && registrar == null) {
            // powerjob 引擎或 lite 形态：调度器不在本服务手上，只允许落表留档
            log.warn("[job-admin] 本实例无 simple 调度器（engine=none 或未装配），仅落表: {}", jobKey);
        } else if (!registered) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "任务不存在或未注册: " + jobKey);
        }
        SysJob sysJob = sysJobRepository.findByJobKeyAndDeletedFalse(jobKey).orElseGet(() -> {
            SysJob created = new SysJob();
            created.setJobKey(jobKey);
            return created;
        });
        sysJob.setCron(cron);
        sysJob.setEnabled(enabled);
        sysJob.setRemark(remark);
        sysJobRepository.save(sysJob);
    }

    /** 立即触发一次（异步执行，锁/执行日志语义与定时触发一致） */
    public void trigger(String jobKey) {
        JobRegistrar registrar = registrarProvider.getIfAvailable();
        if (registrar == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "本实例无 simple 调度器，无法触发");
        }
        if (!registrar.triggerNow(jobKey)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "任务不存在或未注册: " + jobKey);
        }
    }

    /** 执行日志分页（jobName 可空 = 全部任务混排，最新在前） */
    public PageResult<JobRunLog> runLogs(String jobName, int pageNum, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(pageNum - 1, 0), Math.min(pageSize, 100));
        Page<JobRunLog> page = (StringUtils.isBlank(jobName))
                ? runLogRepository.findAllByDeletedFalseOrderByIdDesc(request)
                : runLogRepository.findByJobNameAndDeletedFalseOrderByIdDesc(jobName.trim(), request);
        return PageResult.of(page.getContent(), page.getTotalElements(), pageNum, pageSize);
    }

    private Map<String, Object> base(String jobKey, String jobName, String className) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("jobKey", jobKey);
        view.put("jobName", jobName);
        view.put("className", className);
        view.put("configured", false);
        view.put("configCron", null);
        view.put("configEnabled", null);
        view.put("remark", null);
        return view;
    }
}
