package com.scaffold.starter.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 接口文档（knife4j / springdoc-openapi）自动装配。
 * 生产环境通过配置 knife4j.production=true 关闭（见 application-prod.yml）。
 */
@ConditionalOnClass(OpenAPI.class)
public class OpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI scaffoldOpenApi(Environment environment) {
        String appName = environment.getProperty("spring.application.name", "scaffold");
        return new OpenAPI().info(new Info()
                .title(appName + " 接口文档")
                .description("基于 scaffold 脚手架：/internal 内部管理端 · /openapi 三方接入 · /online 线上端")
                .version(environment.getProperty("scaffold.doc.version", "1.0.0")));
    }
}
