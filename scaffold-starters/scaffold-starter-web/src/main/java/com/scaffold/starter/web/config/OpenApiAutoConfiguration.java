package com.scaffold.starter.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 接口文档自动装配器，为 knife4j / springdoc-openapi 提供默认 {@code OpenAPI} 描述。
 *
 * <p>仅当类路径存在 {@code OpenAPI} 时生效，且容器中尚无同名 Bean 才注册；
 * 标题取 {@code spring.application.name}，版本取 {@code scaffold.doc.version}。</p>
 *
 * <p>生产环境通过 {@code knife4j.production=true} 关闭文档（见 application-prod.yml），
 * 避免接口结构对外暴露。</p>
 *
 * @author lianghang
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
