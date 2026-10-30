package com.scaffold.starter.job;

import java.util.Optional;

/**
 * 任务配置的外部提供方 SPI：把「数据驱动」从 starter 解耦出去。
 *
 * <p>simple 引擎的 cron/启停默认来自代码注解；引入方（如 module-system 落 sys_job 表）
 * 实现本接口注册为 Bean，即可用表配置覆盖代码默认值。starter 不依赖数据库——
 * 无提供方（如 server-lite 零依赖形态）时自动回退纯代码驱动，业务零感知。</p>
 *
 * <p>回退链：表配置 &gt; 代码注解默认值；运行时改配置经
 * {@link JobRegistrar#apply(String, String, boolean)} 动态生效（不重启）。</p>
 *
 * @author lianghang
 */
public interface JobConfigProvider {

    /**
     * 查询某任务的外部配置。
     *
     * @param jobKey 任务标识（@Job 注解任务的唯一键）
     * @return 覆盖配置；无配置返回 empty（用代码默认值）
     */
    Optional<JobOverride> find(String jobKey);

    /** 外部配置值对象：cron 与启停开关 */
    class JobOverride {

        private final String cron;
        private final boolean enabled;

        public JobOverride(String cron, boolean enabled) {
            this.cron = cron;
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
