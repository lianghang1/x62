package com.scaffold.starter.web.config;

import com.scaffold.common.util.CollUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.scaffold.common.util.DateUtils;
import com.scaffold.starter.web.async.AsyncExceptionHandler;
import com.scaffold.starter.web.exception.GlobalExceptionHandler;
import com.scaffold.starter.web.log.AccessLogAspect;
import com.scaffold.starter.web.trace.TraceIdFilter;
import com.scaffold.starter.web.trace.TraceTaskDecorator;
import com.scaffold.starter.web.xss.XssFilter;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * starter-web 自动装配入口，统一注册 Web 层的基础设施 Bean。
 *
 * <p>覆盖四类横切关注点：traceId 与 XSS 两个全局 Filter、Jackson 序列化规约、
 * 访问日志与异常处理，外加 CORS 与 {@code @Async} 线程池装饰器。Jackson 侧
 * 将 Long 序列化为字符串以规避 JS 精度丢失，时间统一 {@code yyyy-MM-dd HH:mm:ss}、
 * 时区 GMT+8。</p>
 *
 * <p>安全边界默认收紧：XSS 过滤默认开启（可按路径排除），CORS 仅在显式配置
 * {@code allowed-origins} 时生效，禁止全放开。</p>
 *
 * @author lianghang
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableAsync
@EnableConfigurationProperties({WebProperties.class, BizExecutorProperties.class})
public class WebAutoConfiguration {

    /** @deprecated 全局格式常量已收口 {@link DateUtils}，新代码直接引用 */
    @Deprecated
    public static final String DATE_TIME_PATTERN = DateUtils.DATE_TIME_PATTERN;
    /** @deprecated 全局格式常量已收口 {@link DateUtils} */
    @Deprecated
    public static final String DATE_PATTERN = DateUtils.DATE_PATTERN;
    public static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * @Async 专属池 {@code scaffoldAsyncExecutor}：经 AsyncConfigurer 显式指定，不依赖
     * Boot 默认 applicationTaskExecutor（后者只要出现任意 Executor Bean 就会退避）。
     * 业务定义同名 Bean 即整体接管。
     */
    @Bean(name = "scaffoldAsyncExecutor")
    @ConditionalOnMissingBean(name = "scaffoldAsyncExecutor")
    public ThreadPoolTaskExecutor scaffoldAsyncExecutor(BizExecutorProperties executorProperties) {
        BizExecutorProperties.Async async = executorProperties.getAsync();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(async.getCoreSize());
        executor.setMaxPoolSize(async.getMaxSize());
        executor.setQueueCapacity(async.getQueueCapacity());
        executor.setThreadNamePrefix(async.getNamePrefix());
        executor.setTaskDecorator(new TraceTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(async.getAwaitTerminationSeconds());
        return executor;
    }

    /**
     * @Async 异常兜底 + 显式执行器：void 方法未捕获异常补齐方法签名与入参记录，
     * 异步任务静默失败可定位；执行器指定 {@code scaffoldAsyncExecutor}。
     */
    @Bean
    public AsyncConfigurer scaffoldAsyncConfigurer(ThreadPoolTaskExecutor scaffoldAsyncExecutor) {
        return new AsyncConfigurer() {
            @Override
            public org.springframework.core.task.AsyncTaskExecutor getAsyncExecutor() {
                return scaffoldAsyncExecutor;
            }

            @Override
            public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return new AsyncExceptionHandler();
            }
        };
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(WebProperties properties) {
        FilterRegistrationBean<TraceIdFilter> registration =
                new FilterRegistrationBean<>(new TraceIdFilter(
                        properties.isAccessLogEnabled() && properties.isAccessLogStartLine()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        return registration;
    }

    /** XSS 过滤：参数与 body 尖括号转义，紧跟 TraceIdFilter 之后。 */
    @Bean
    @ConditionalOnProperty(prefix = "scaffold.web.xss", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<XssFilter> xssFilterRegistration(WebProperties properties) {
        FilterRegistrationBean<XssFilter> registration =
                new FilterRegistrationBean<>(new XssFilter(properties.getXss().getExcludeUrls()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        return registration;
    }

    /**
     * Jackson 统一配置：Long/long 序列化为字符串（防止 JS 精度丢失），时间统一格式、时区 GMT+8。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer scaffoldJacksonCustomizer() {
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter date = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter time = DateTimeFormatter.ofPattern(TIME_PATTERN);
        return builder -> {
            builder.simpleDateFormat(DATE_TIME_PATTERN);
            builder.timeZone("GMT+8");
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTime));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(date));
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(time));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTime));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(date));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(time));
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnProperty(prefix = "scaffold.web", name = "access-log-enabled", havingValue = "true", matchIfMissing = true)
    public AccessLogAspect accessLogAspect(ObjectMapper objectMapper) {
        return new AccessLogAspect(objectMapper);
    }

    /**
     * Boot 默认 applicationTaskExecutor 的兜底加固：仅当它真实存在时生效（如业务关闭
     * biz 池且接管 async 池后 Boot 池可能仍在），保证装饰器与有界队列不缺席。
     */
    @Bean
    public TaskExecutorCustomizer traceTaskExecutorCustomizer(BizExecutorProperties executorProperties) {
        return taskExecutor -> {
            taskExecutor.setTaskDecorator(new TraceTaskDecorator());
            taskExecutor.setQueueCapacity(executorProperties.getAsync().getQueueCapacity());
            taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        };
    }

    /**
     * 业务并行池：service 方法内需要多线程时直接注入使用（按名注入保证唯一）。
     *
     * <p>预置四件事：TraceTaskDecorator（TTL 上下文 + MDC traceId 透传）、有界队列、
     * CallerRunsPolicy（队列打满时任务回落到提交线程执行，背压且不丢任务）、
     * 优雅关闭。业务定义同名 Bean 即整体接管本配置。</p>
     *
     * <p>两条边界（详见 README「业务侧多线程规约」）：子任务事务与提交线程无关，
     * 需要事务的在子任务上单独标注；{@code Future.get} 必须设超时。</p>
     */
    @Bean(name = "scaffoldBizExecutor")
    @ConditionalOnProperty(prefix = "scaffold.executor.biz", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "scaffoldBizExecutor")
    public ThreadPoolTaskExecutor scaffoldBizExecutor(BizExecutorProperties executorProperties) {
        BizExecutorProperties.Biz biz = executorProperties.getBiz();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(biz.getCoreSize());
        executor.setMaxPoolSize(biz.getMaxSize());
        executor.setQueueCapacity(biz.getQueueCapacity());
        executor.setThreadNamePrefix(biz.getNamePrefix());
        executor.setTaskDecorator(new TraceTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(biz.getAwaitTerminationSeconds());
        return executor;
    }

    /** CORS：仅当配置了 allowed-origins 时生效，默认关闭。 */
    @Bean
    public WebMvcConfigurer scaffoldCorsConfigurer(final WebProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                WebProperties.Cors cors = properties.getCors();
                if (CollUtils.isEmpty(cors.getAllowedOrigins())) {
                    return;
                }
                registry.addMapping("/**")
                        .allowedOrigins(cors.getAllowedOrigins().toArray(new String[0]))
                        .allowedMethods(methods(cors))
                        .allowedHeaders(cors.getAllowedHeaders().toArray(new String[0]))
                        .maxAge(cors.getMaxAge());
            }
        };
    }

    private String[] methods(WebProperties.Cors cors) {
        return CollUtils.isEmpty(cors.getAllowedMethods())
                ? new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS"}
                : cors.getAllowedMethods().toArray(new String[0]);
    }
}
