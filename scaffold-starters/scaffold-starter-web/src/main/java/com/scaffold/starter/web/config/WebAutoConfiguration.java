package com.scaffold.starter.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.scaffold.starter.web.exception.GlobalExceptionHandler;
import com.scaffold.starter.web.log.AccessLogAspect;
import com.scaffold.starter.web.trace.TraceIdFilter;
import com.scaffold.starter.web.trace.TraceTaskDecorator;
import com.scaffold.starter.web.xss.XssFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * starter-web 自动装配入口。
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(WebProperties.class)
public class WebAutoConfiguration {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration =
                new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        return registration;
    }

    /** XSS 过滤：参数与 body 尖括号转义，紧跟 TraceIdFilter 之后 */
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
     * Jackson 统一配置：
     * Long/long 序列化为字符串（防止 JS 精度丢失）；时间统一格式；时区 GMT+8。
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

    /** @Async 默认线程池：统一挂上 traceId 装饰器（LoginContext 透传由 TTL 提供，见 starter-security） */
    @Bean
    public TaskExecutorCustomizer traceTaskExecutorCustomizer() {
        return taskExecutor -> taskExecutor.setTaskDecorator(new TraceTaskDecorator());
    }

    /** CORS：仅当配置了 allowed-origins 时生效，默认关闭 */
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
