package com.scaffold.starter.web.config;

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
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
@EnableConfigurationProperties(WebProperties.class)
public class WebAutoConfiguration {

    /** @deprecated 全局格式常量已收口 {@link DateUtils}，新代码直接引用 */
    @Deprecated
    public static final String DATE_TIME_PATTERN = DateUtils.DATE_TIME_PATTERN;
    /** @deprecated 全局格式常量已收口 {@link DateUtils} */
    @Deprecated
    public static final String DATE_PATTERN = DateUtils.DATE_PATTERN;
    public static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * @Async 异常兜底：不接管执行器（返回 null 走 Boot 默认 applicationTaskExecutor，
     * 由下方 TaskExecutorCustomizer 继续生效），只替换未捕获异常处理器。
     */
    @Bean
    public AsyncConfigurer scaffoldAsyncConfigurer() {
        return new AsyncConfigurer() {
            @Override
            public org.springframework.core.task.AsyncTaskExecutor getAsyncExecutor() {
                return null;
            }

            @Override
            public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return new AsyncExceptionHandler();
            }
        };
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration =
                new FilterRegistrationBean<>(new TraceIdFilter());
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

    /** @Async 默认线程池：装饰器同时透传 MDC traceId 与 TTL 上下文（LoginContext/审计人），自建池请用 ScaffoldExecutors 包装。 */
    @Bean
    public TaskExecutorCustomizer traceTaskExecutorCustomizer() {
        return taskExecutor -> taskExecutor.setTaskDecorator(new TraceTaskDecorator());
    }

    /** CORS：仅当配置了 allowed-origins 时生效，默认关闭。 */
    @Bean
    public WebMvcConfigurer scaffoldCorsConfigurer(final WebProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                WebProperties.Cors cors = properties.getCors();
                if (cors.getAllowedOrigins() == null || cors.getAllowedOrigins().isEmpty()) {
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
        return cors.getAllowedMethods() == null || cors.getAllowedMethods().isEmpty()
                ? new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS"}
                : cors.getAllowedMethods().toArray(new String[0]);
    }
}
