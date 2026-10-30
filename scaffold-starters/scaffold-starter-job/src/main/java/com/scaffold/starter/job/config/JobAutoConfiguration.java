package com.scaffold.starter.job.config;

import com.scaffold.starter.job.JobRunLogRepository;
import com.scaffold.starter.protection.lock.RedisLockUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 计划任务自动装配（极简引擎）。
 * 接入 PowerJob 时：server 不引本 starter 的注册逻辑（scaffold.job.engine=none），
 * 由 PowerJob 适配模块接管调度，AbstractJob 子类零改动。
 */
@Configuration
@EnableConfigurationProperties(JobAutoConfiguration.JobProperties.class)
public class JobAutoConfiguration {

    @ConfigurationProperties(prefix = "scaffold.job")
    public static class JobProperties {

        /** simple = 本注册器；none = 接入外部调度平台 */
        private String engine = "simple";

        /** 调度线程池大小 */
        private int poolSize = 4;

        public String getEngine() {
            return engine;
        }

        public void setEngine(String engine) {
            this.engine = engine;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler jobTaskScheduler(JobProperties properties) {
        return JobRegistrar.initScheduler(properties.getPoolSize());
    }

    @Bean
    public JobRegistrar jobRegistrar(ApplicationContext applicationContext, Environment environment,
                                     TaskScheduler taskScheduler, JobProperties properties,
                                     ObjectProvider<JobRunLogRepository> runLogRepository,
                                     ObjectProvider<RedisLockUtil> lockUtil,
                                     ObjectProvider<TransactionTemplate> transactionTemplate) {
        if ("none".equalsIgnoreCase(properties.getEngine())) {
            // 外部调度平台接管：不注册，但仍安装 Recorder 供执行日志使用
            return null;
        }
        return new JobRegistrar(applicationContext, environment, taskScheduler,
                runLogRepository.getIfAvailable(), lockUtil.getIfAvailable(),
                transactionTemplate.getIfAvailable());
    }
}
