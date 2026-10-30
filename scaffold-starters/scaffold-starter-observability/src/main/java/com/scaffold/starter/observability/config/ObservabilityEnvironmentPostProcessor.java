package com.scaffold.starter.observability.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 可观测性默认配置注入：在用户配置之前提供合理的端点暴露默认值。
 *
 * <p>PropertySource 以 {@code addLast} 挂最低优先级——业务方在 application.yml
 * 写任意同名 key 即整体覆盖，框架只在用户「什么都没配」时给默认：暴露
 * health/info/prometheus 三个端点。安全边界：prometheus 指标不含敏感业务
 * 数据，生产环境建议由网关/安全组限制 actuator 路径的外网访问（README 有说明）。</p>
 *
 * <p>注册于 META-INF/spring.factories；{@code @Order(LOWEST)} 保证在
 * ConfigData（application.yml 解析）之后执行，避免抢占用户配置。</p>
 *
 * @author lianghang
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class ObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "scaffoldObservabilityDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(SOURCE_NAME)) {
            return;
        }
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("management.endpoints.web.exposure.include", "health,info,prometheus");
        defaults.put("management.endpoint.health.show-details", "when-authorized");
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }
}
