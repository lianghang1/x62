package com.scaffold.starter.protection.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，基于 Lua 原子计数实现滑动窗口语义。
 *
 * <p>计数与过期由 Redis EXPIRE 加 INCR 的原子脚本保证；默认维度为 URI，
 * 指定 dimension="account" 时按当前账户 ID 限流（匿名按 IP）——限流粒度按需声明。</p>
 *
 * @author lianghang
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内允许的最大请求数 */
    int limit();

    /** 窗口大小（秒） */
    int windowSeconds() default 1;

    /** 限流维度：uri（默认）/ account / ip */
    String dimension() default "uri";

    /** 自定义限流 key 后缀（与维度值组合） */
    String key() default "";
}
