package com.scaffold.starter.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.starter.security.filter.InternalAuthenticationFilter;
import com.scaffold.starter.security.filter.OnlineTokenFilter;
import com.scaffold.starter.security.filter.OpenApiSignFilter;
import com.scaffold.starter.security.openapi.AppSecretProvider;
import com.scaffold.starter.security.token.RedisTokenService;
import com.scaffold.starter.security.token.TokenService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 三入口安全体系的自动装配类，一条链一个配置方法，白名单唯一出自 SecurityProperties。
 *
 * <p>链路按 Order 分层：@Order(1) /openapi/** 走 HMAC 签名认证（APP 账户）；
 * @Order(2) /internal/** 走 token 会话加 RBAC 方法鉴权（OPERATOR）；
 * @Order(3) 其余请求归 online 链——permitAll 加可选 token（MEMBER）。互不影响、各司其职。</p>
 *
 * @author lianghang
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(SecurityProperties.class)
public class ScaffoldSecurityAutoConfiguration {

    // ---------------- 公共设施 ----------------

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(TokenService.class)
    public TokenService tokenService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                     SecurityProperties properties) {
        return new RedisTokenService(redisTemplate, objectMapper,
                properties.getTokenTtlSeconds(), properties.getRenewThresholdPercent());
    }

    /** 登录失败锁定：internal 账密登录在 service 层调用（记录/判定/清零），账号 + IP 双维度 */
    @Bean
    @ConditionalOnMissingBean(com.scaffold.starter.security.policy.LoginFailureGuard.class)
    public com.scaffold.starter.security.policy.LoginFailureGuard loginFailureGuard(
            StringRedisTemplate redisTemplate, SecurityProperties properties,
            org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistry) {
        return new com.scaffold.starter.security.policy.LoginFailureGuard(
                redisTemplate, properties.getPasswordPolicy(), properties.getIpGuard(),
                meterRegistry.getIfAvailable());
    }

    /** 图形验证码（Hutool + Redis 一次性），internal 登录防机器撞库 */
    @Bean
    @ConditionalOnMissingBean(com.scaffold.starter.security.captcha.CaptchaService.class)
    public com.scaffold.starter.security.captcha.CaptchaService captchaService(
            StringRedisTemplate redisTemplate, SecurityProperties properties) {
        return new com.scaffold.starter.security.captcha.CaptchaService(
                redisTemplate, properties.getCaptcha());
    }

    /** 密码强度校验（创建账号/改密时显式调用） */
    @Bean
    @ConditionalOnMissingBean(com.scaffold.starter.security.policy.PasswordPolicyValidator.class)
    public com.scaffold.starter.security.policy.PasswordPolicyValidator passwordPolicyValidator(
            SecurityProperties properties) {
        return new com.scaffold.starter.security.policy.PasswordPolicyValidator(
                properties.getPasswordPolicy());
    }

    /** @ApiEncrypt 接口加解密：注解驱动，未标注接口零影响；密文经 DataCipher（starter-crypto 装配） */
    @Bean
    public com.scaffold.starter.security.apiencrypt.ApiEncryptRequestBodyAdvice apiEncryptRequestBodyAdvice() {
        return new com.scaffold.starter.security.apiencrypt.ApiEncryptRequestBodyAdvice();
    }

    @Bean
    public com.scaffold.starter.security.apiencrypt.ApiEncryptResponseBodyAdvice apiEncryptResponseBodyAdvice(
            ObjectMapper objectMapper) {
        return new com.scaffold.starter.security.apiencrypt.ApiEncryptResponseBodyAdvice(objectMapper);
    }

    /** 401/403 统一返回 JSON（不带登录页重定向） */
    private void restExceptionHandling(HttpSecurity http) throws Exception {
        http.exceptionHandling()
                .authenticationEntryPoint((request, response, e) -> write(response, GlobalErrorCode.UNAUTHORIZED))
                .accessDeniedHandler((request, response, e) -> write(response, GlobalErrorCode.FORBIDDEN));
    }

    private void write(HttpServletResponse response, GlobalErrorCode errorCode) throws java.io.IOException {
        response.setStatus(errorCode.getHttpStatus() == -1 ? 200 : errorCode.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + errorCode.getCode()
                + ",\"message\":\"" + errorCode.getMessage()
                + "\",\"traceId\":\"" + org.slf4j.MDC.get("traceId") + "\"}");
        response.getWriter().flush();
    }

    // ---------------- 链①：/openapi/** ----------------

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain openApiChain(HttpSecurity http,
                                            ObjectProvider<AppSecretProvider> provider,
                                            SecurityProperties properties,
                                            StringRedisTemplate redisTemplate) throws Exception {
        OpenApiSignFilter signFilter = new OpenApiSignFilter(
                provider.getIfAvailable() != null ? provider.getIfAvailable() : denyAllProvider(),
                properties, redisTemplate);
        http.antMatcher("/openapi/**")
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling().authenticationEntryPoint(
                        (request, response, e) -> write(response, GlobalErrorCode.UNAUTHORIZED))
                .and()
                .authorizeRequests().anyRequest().authenticated()
                .and()
                .addFilterBefore(signFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** fail closed：业务方未提供 AppSecretProvider 时，openapi 全部拒绝 */
    private AppSecretProvider denyAllProvider() {
        return appKey -> null;
    }

    // ---------------- 链②：/internal/** ----------------

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain internalChain(HttpSecurity http, TokenService tokenService,
                                             SecurityProperties properties) throws Exception {
        String[] whitelist = properties.getInternalWhitelist().toArray(new String[0]);
        http.antMatcher("/internal/**")
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(whitelist).permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, e) -> write(response, GlobalErrorCode.UNAUTHORIZED))
                .accessDeniedHandler((request, response, e) -> write(response, GlobalErrorCode.FORBIDDEN))
                .and()
                .addFilterBefore(new InternalAuthenticationFilter(tokenService, properties),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // ---------------- 链③：online（默认 permitAll） ----------------

    @Bean
    @org.springframework.core.annotation.Order(3)
    public SecurityFilterChain onlineChain(HttpSecurity http, TokenService tokenService,
                                           SecurityProperties properties) throws Exception {
        http.antMatcher("/**")
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests().anyRequest().permitAll()
                .and()
                .addFilterBefore(new OnlineTokenFilter(tokenService, properties),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 权限校验拦截器：对声明 {@code @RequirePermission} 的管理接口做 any-of 校验，
     * 无注解接口零开销直通。执行于认证过滤器之后，LoginContext 已就绪。
     */
    @Bean
    @ConditionalOnMissingBean(com.scaffold.starter.security.permission.PermissionInterceptor.class)
    public org.springframework.web.servlet.HandlerInterceptor permissionInterceptor() {
        return new com.scaffold.starter.security.permission.PermissionInterceptor();
    }

    @Bean
    public org.springframework.web.servlet.config.annotation.WebMvcConfigurer permissionInterceptorConfigurer(
            org.springframework.web.servlet.HandlerInterceptor permissionInterceptor) {
        return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
            @Override
            public void addInterceptors(
                    org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
                registry.addInterceptor(permissionInterceptor).addPathPatterns("/**");
            }
        };
    }
}
