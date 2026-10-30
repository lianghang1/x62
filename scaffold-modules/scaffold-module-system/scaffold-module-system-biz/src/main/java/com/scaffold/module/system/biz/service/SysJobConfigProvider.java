package com.scaffold.module.system.biz.service;

import com.scaffold.module.system.biz.entity.SysJob;
import com.scaffold.module.system.biz.repository.SysJobRepository;
import com.scaffold.starter.job.JobConfigProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 表配置到调度引擎的桥接：sys_job → JobConfigProvider SPI。
 *
 * <p>启动时 JobRegistrar 逐任务调 {@link #find(String)} 取表配置覆盖代码默认值；
 * 本类只读不写——运行时变更由 JobAdminService 落表后调 JobRegistrar.apply 动态生效。</p>
 *
 * @author lianghang
 */
@Service
public class SysJobConfigProvider implements JobConfigProvider {

    private final SysJobRepository repository;

    public SysJobConfigProvider(SysJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<JobOverride> find(String jobKey) {
        return repository.findByJobKeyAndDeletedFalse(jobKey)
                .map(job -> new JobOverride(job.getCron(), Boolean.TRUE.equals(job.getEnabled())));
    }
}
