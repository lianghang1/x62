package com.scaffold.starter.protection.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解：Redis setIfAbsent + 唯一持有值 + Lua 校验删除（防误删他人锁）。
 * SpEL 支持：key 支持 #p0 #result 等占位（通过 SpelKeyResolver 解析）。
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
