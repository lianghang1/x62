package com.scaffold.starter.job.config;

import com.scaffold.starter.job.JobConfigProvider;
import com.scaffold.starter.job.JpaJobRunLogSink;
import com.scaffold.starter.job.JobRunLogRepository;
import com.scaffold.starter.job.JobRunLogSink;
import com.scaffold.starter.protection.lock.RedisLockUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
                                     ObjectProvider<JobRunLogSink> runLogSink,
                                     ObjectProvider<RedisLockUtil> lockUtil,
                                     ObjectProvider<JobConfigProvider> configProvider) {
        if ("none".equalsIgnoreCase(properties.getEngine())) {
            // 外部调度平台接管：不注册，运行时管理走平台自带控制台
            return null;
        }
        return new JobRegistrar(applicationContext, environment, taskScheduler,
                runLogSink.getIfAvailable(), lockUtil.getIfAvailable(),
                configProvider.getIfAvailable());
    }

    /**
     * 执行日志的 JPA 落库装配，仅在类路径存在 JPA 时生效。
     *
     * <p>无数据库场景（如 server-lite 单机零依赖运行）下本嵌套装配整体跳过，
     * JPA 相关类型不加载，调度链路退化为纯应用日志。</p>
     */
    @Configuration
    @ConditionalOnClass(name = "javax.persistence.EntityManager")
    public static class JpaRunLogConfiguration {

        @Bean
        @ConditionalOnMissingBean(JobRunLogSink.class)
        public JobRunLogSink jobRunLogSink(ObjectProvider<JobRunLogRepository> runLogRepository,
                                           ObjectProvider<TransactionTemplate> transactionTemplate) {
            return new JpaJobRunLogSink(runLogRepository.getIfAvailable(),
                    transactionTemplate.getIfAvailable());
        }
    }
}
