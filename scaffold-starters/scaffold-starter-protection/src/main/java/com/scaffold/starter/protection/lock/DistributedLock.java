package com.scaffold.starter.protection.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解，基于 Redis setIfAbsent 加唯一持有值实现。
 *
 * <p>解锁经 Lua 校验持有值后删除，防止误删他人锁；key 支持 #p0、#result 等 SpEL 占位
 * （通过 SpelKeyResolver 解析）——锁粒度精确到业务对象而非整个接口。</p>
 *
 * @author lianghang
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /** 锁 key 前缀（自动拼接 SpEL 表达式值） */
    String key();

    /** 等待获取锁的最长时间（毫秒），0 = 不等待，拿不到立即抛错 */
    long waitMillis() default 0;

    /** 锁自动释放时间（秒），防止持锁节点宕机死锁 */
    int leaseSeconds() default 30;
}
