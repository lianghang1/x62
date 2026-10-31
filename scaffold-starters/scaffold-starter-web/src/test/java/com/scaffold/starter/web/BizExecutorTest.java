package com.scaffold.starter.web;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.scaffold.starter.web.config.BizExecutorProperties;
import com.scaffold.starter.web.config.WebAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 预置线程池（业务并行池 scaffoldBizExecutor、@Async 专属池 scaffoldAsyncExecutor）行为测试。
 *
 * <p>基于 WebApplicationContextRunner 独立上下文、零外部依赖：覆盖默认参数、
 * TTL 上下文与 MDC traceId 透传、CallerRunsPolicy 背压、enabled 开关、
 * 业务同名 Bean 接管、@Async 经 AsyncConfigurer 显式绑定。</p>
 *
 * @author lianghang
 */
class BizExecutorTest {

    /** 测试专用 TTL 上下文：验证透传机制用（与 LoginContext 同底座）。 */
    private static final TransmittableThreadLocal<String> TEST_TTL = new TransmittableThreadLocal<>();

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class, JacksonAutoConfiguration.class));

    @AfterEach
    void cleanContext() {
        TEST_TTL.remove();
        MDC.clear();
    }

    @Test
    void defaultBizExecutorShouldBeRegisteredWithSaneDefaults() {
        runner.run(context -> {
            assertThat(context).hasBean("scaffoldBizExecutor");
            ThreadPoolTaskExecutor executor = context.getBean("scaffoldBizExecutor", ThreadPoolTaskExecutor.class);
            assertThat(executor.getCorePoolSize()).isEqualTo(4);
            assertThat(executor.getMaxPoolSize()).isEqualTo(8);
            assertThat(executor.getQueueCapacity()).isEqualTo(1000);
            assertThat(executor.getThreadNamePrefix()).isEqualTo("scaffold-biz-");
            ThreadPoolExecutor raw = executor.getThreadPoolExecutor();
            assertThat(raw.getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
            assertThat(raw.getQueue()).isInstanceOf(LinkedBlockingQueue.class);
        });
    }

    @Test
    void ttlContextShouldPropagateIntoBizPoolTasks() {
        runner.run(context -> {
            ThreadPoolTaskExecutor executor = context.getBean("scaffoldBizExecutor", ThreadPoolTaskExecutor.class);
            TEST_TTL.set("user-42");
            try {
                Future<String> future = executor.submit(TEST_TTL::get);
                assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("user-42");
            } finally {
                TEST_TTL.remove();
            }
        });
    }

    @Test
    void mdcTraceIdShouldPropagateIntoBizPoolTasks() {
        runner.run(context -> {
            ThreadPoolTaskExecutor executor = context.getBean("scaffoldBizExecutor", ThreadPoolTaskExecutor.class);
            MDC.put("traceId", "trace-abc");
            try {
                Callable<Map<String, String>> snapshot = MDC::getCopyOfContextMap;
                Future<Map<String, String>> future = executor.submit(snapshot);
                assertThat(future.get(5, TimeUnit.SECONDS)).containsEntry("traceId", "trace-abc");
            } finally {
                MDC.remove("traceId");
            }
        });
    }

    @Test
    void queueOverflowShouldFallBackToCallerThread() {
        WebApplicationContextRunner tight = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class, JacksonAutoConfiguration.class))
                .withPropertyValues("scaffold.executor.biz.core-size=1",
                        "scaffold.executor.biz.max-size=1",
                        "scaffold.executor.biz.queue-capacity=1");
        tight.run(context -> {
            ThreadPoolTaskExecutor executor = context.getBean("scaffoldBizExecutor", ThreadPoolTaskExecutor.class);
            TEST_TTL.set("caller-thread-user");
            try {
                // 占住唯一工作线程并塞满队列（1），下一个任务必触发 CallerRunsPolicy 回到提交线程
                Future<?> blocker = executor.submit(() -> await(300));
                executor.submit(() -> await(100));
                Future<String> rejected = executor.submit(TEST_TTL::get);
                // 回落到提交线程执行：TTL 值即提交线程本身持有的值
                assertThat(rejected.get(5, TimeUnit.SECONDS)).isEqualTo("caller-thread-user");
                blocker.cancel(true);
            } finally {
                TEST_TTL.remove();
            }
        });
    }

    @Test
    void disabledShouldNotRegisterBizExecutor() {
        WebApplicationContextRunner off = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class, JacksonAutoConfiguration.class))
                .withPropertyValues("scaffold.executor.biz.enabled=false");
        off.run(context -> {
            assertThat(context).doesNotHaveBean("scaffoldBizExecutor");
            // @Async 专属池不受 biz 开关影响
            assertThat(context).hasBean("scaffoldAsyncExecutor");
        });
    }

    @Test
    void userDefinedBeanShouldTakeOver() {
        // withBean 先于配置类解析注册 Bean 定义，@ConditionalOnMissingBean 评估时必然可见，
        // 等价于真实 Boot 中「用户配置先于自动装配」的语义
        WebApplicationContextRunner override = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class, JacksonAutoConfiguration.class))
                .withBean("scaffoldBizExecutor", ThreadPoolTaskExecutor.class, CustomPoolConfig::custom);
        override.run(context -> {
            assertThat(context).hasBean("scaffoldBizExecutor");
            ThreadPoolTaskExecutor executor = context.getBean("scaffoldBizExecutor", ThreadPoolTaskExecutor.class);
            assertThat(executor.getCorePoolSize()).isEqualTo(2);
            assertThat(executor.getThreadNamePrefix()).isEqualTo("my-pool-");
        });
    }

    @Test
    void asyncExecutorShouldBeBoundViaAsyncConfigurerAndBounded() {
        runner.run(context -> {
            ThreadPoolTaskExecutor asyncPool =
                    context.getBean("scaffoldAsyncExecutor", ThreadPoolTaskExecutor.class);
            assertThat(asyncPool.getCorePoolSize()).isEqualTo(8);
            assertThat(asyncPool.getMaxPoolSize()).isEqualTo(16);
            assertThat(asyncPool.getQueueCapacity()).isEqualTo(1000);
            assertThat(asyncPool.getThreadNamePrefix()).isEqualTo("scaffold-async-");
            assertThat(asyncPool.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
            // AsyncConfigurer 显式绑定该池，@Async 不再依赖 Boot 默认池的存在性
            assertThat(context.getBean(org.springframework.scheduling.annotation.AsyncConfigurer.class))
                    .extracting(configurer -> configurer.getAsyncExecutor())
                    .isEqualTo(asyncPool);
        });
    }

    @Test
    void userDefinedAsyncBeanShouldTakeOver() {
        WebApplicationContextRunner override = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class, JacksonAutoConfiguration.class))
                .withBean("scaffoldAsyncExecutor", ThreadPoolTaskExecutor.class, CustomAsyncConfig::custom);
        override.run(context -> {
            ThreadPoolTaskExecutor asyncPool =
                    context.getBean("scaffoldAsyncExecutor", ThreadPoolTaskExecutor.class);
            assertThat(asyncPool.getThreadNamePrefix()).isEqualTo("my-async-");
            assertThat(context.getBean(org.springframework.scheduling.annotation.AsyncConfigurer.class))
                    .extracting(configurer -> configurer.getAsyncExecutor())
                    .isEqualTo(asyncPool);
        });
    }

    @Test
    void propertiesDefaultsShouldMatchDesign() {
        BizExecutorProperties properties = new BizExecutorProperties();
        assertThat(properties.getBiz().isEnabled()).isTrue();
        assertThat(properties.getBiz().getQueueCapacity()).isEqualTo(1000);
        assertThat(properties.getAsync().getQueueCapacity()).isEqualTo(1000);
    }

    private static void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 业务自定义业务池实例（经 withBean 注册，验证 @ConditionalOnMissingBean 退避）。 */
    static class CustomPoolConfig {

        static ThreadPoolTaskExecutor custom() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setThreadNamePrefix("my-pool-");
            return executor;
        }
    }

    /** 业务自定义 @Async 池实例，验证 AsyncConfigurer 绑定随之切换。 */
    static class CustomAsyncConfig {

        static ThreadPoolTaskExecutor custom() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("my-async-");
            return executor;
        }
    }
}
