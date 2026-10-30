package com.scaffold.server.lite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 轻量版启动类：单机零依赖运行（无数据库、无 Redis）。
 *
 * <p>只装配 web + job 两个 starter：任务按 {@code @Job} 注解的 cron 调度，
 * 执行日志退化为应用日志，锁/限流/幂等注解静默不生效。完整能力（账户、缓存、
 * 推送、防刷）请使用 scaffold-server。</p>
 *
 * <p>{@code scanBasePackages} 只扫自身 demo 代码；starter 一律走自动装配
 * （imports 文件），与 scaffold-server 同一约定——避免「扫到即装配」绕过条件注解。</p>
 *
 * @author lianghang
 */
@SpringBootApplication(scanBasePackages = "com.scaffold.server.lite")
public class LiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteApplication.class, args);
    }
}
