package com.scaffold.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类。
 * scanBasePackages 只扫业务模块与自身 demo 代码；starter 一律走自动装配（imports 文件），
 * 避免「扫到即装配」绕过条件注解——这是 starter 可插拔的关键。
 */
@SpringBootApplication(scanBasePackages = {"com.scaffold.module", "com.scaffold.server"})
public class ScaffoldApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScaffoldApplication.class, args);
    }
}
