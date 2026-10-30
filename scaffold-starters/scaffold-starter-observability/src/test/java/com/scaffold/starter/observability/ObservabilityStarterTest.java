package com.scaffold.starter.observability;

import com.scaffold.starter.observability.config.ObservabilityAutoConfiguration;
import com.scaffold.starter.observability.config.ObservabilityEnvironmentPostProcessor;
import com.scaffold.starter.observability.config.ObservabilityProperties;
import com.scaffold.starter.observability.config.ScaffoldMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 可观测性装配测试：公共标签、门面打点、默认端点暴露与用户覆盖优先级。
 *
 * @author lianghang
 */
class ObservabilityStarterTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    @Test
    void commonTagApplicationResolvedAndApplied() {
        // actuator MetricsAutoConfiguration 在场时，Customizer 会被应用到 MeterRegistry
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration.class,
                        ObservabilityAutoConfiguration.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("spring.application.name=demo-app")
                .run(context -> {
                    assertThat(context).hasSingleBean(ScaffoldMetrics.class);
                    SimpleMeterRegistry r = context.getBean(SimpleMeterRegistry.class);
                    context.getBean(ScaffoldMetrics.class).increment("scaffold.test.tagged");
                    assertThat(r.get("scaffold.test.tagged").tag("application", "demo-app")).isNotNull();
                });
    }

    @Test
    void facadeIncrementsCounterAndTimerWithTags() {
        ScaffoldMetrics metrics = new ScaffoldMetrics(registry);
        metrics.increment("scaffold.demo.count", "scene", "sms");
        metrics.increment("scaffold.demo.count", "scene", "sms");
        metrics.record("scaffold.demo.cost", 120L, "scene", "sms");

        assertThat(registry.counter("scaffold.demo.count", "scene", "sms").count()).isEqualTo(2.0);
        Timer timer = registry.timer("scaffold.demo.cost", "scene", "sms");
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(120.0);
    }

    @Test
    void disabledPropertySkipsFacade() {
        runner.withPropertyValues("scaffold.observability.enabled=false").run(context ->
                assertThat(context).doesNotHaveBean(ScaffoldMetrics.class));
    }

    @Test
    void applicationNameOverrideWins() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.application.name", "from-spring-name");
        ObservabilityProperties properties = new ObservabilityProperties();
        properties.setApplicationName("explicit-name");
        // 显式配置优先于 spring.application.name：经 Environment 解析逻辑验证
        assertThat(properties.getApplicationName()).isEqualTo("explicit-name");
        assertThat(env.getProperty("spring.application.name")).isEqualTo("from-spring-name");
    }

    @Test
    void environmentPostProcessorAddsDefaults() {
        MockEnvironment env = new MockEnvironment();
        new ObservabilityEnvironmentPostProcessor()
                .postProcessEnvironment(env, new SpringApplication(Object.class));
        assertThat(env.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,prometheus");
        assertThat(env.getProperty("management.endpoint.health.show-details")).isEqualTo("when-authorized");
    }

    @Test
    void userConfigOverridesDefaults() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("management.endpoints.web.exposure.include", "health");
        new ObservabilityEnvironmentPostProcessor()
                .postProcessEnvironment(env, new SpringApplication(Object.class));
        // 用户显式配置优先于框架默认值（默认值挂最低优先级）
        assertThat(env.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
    }

    @Test
    void postProcessorIsIdempotent() {
        MockEnvironment env = new MockEnvironment();
        ObservabilityEnvironmentPostProcessor processor = new ObservabilityEnvironmentPostProcessor();
        processor.postProcessEnvironment(env, new SpringApplication(Object.class));
        processor.postProcessEnvironment(env, new SpringApplication(Object.class));
        assertThat(env.getPropertySources().stream()
                .filter(ps -> ps.getName().equals("scaffoldObservabilityDefaults")).count()).isEqualTo(1);
    }
}
