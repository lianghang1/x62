package com.scaffold.starter.job;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 任务元数据注解（配合 AbstractJob 继承体系使用）。
 * cron/enabled 支持配置占位符，改配置不发版即可调整频率或下线任务。
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Job {

    /** 任务名（执行日志与告警展示用） */
    String name();

    /** cron 表达式，支持 ${} 占位，如：${job.order-close.cron:0 0/5 * * * ?} */
    String cron();

    /** 启停开关，支持 ${} 占位，如 ${job.order-close.enabled:true} */
    String enabled() default "true";

    /** 分布式锁自动释放时间（秒）；单机部署可设 0 关闭锁 */
    int lockLeaseSeconds() default 300;
}
