package com.scaffold.starter.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.core.exception.GlobalErrorCode;
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
 * 三入口安全体系装配（一条链一个配置方法，白名单唯一出自 SecurityProperties）：
 * <ol>
 *   <li>@Order(1) /openapi/** —— HMAC 签名认证（APP 账户）；</li>
 *   <li>@Order(2) /internal/** —— token 会话 + RBAC 方法鉴权（OPERATOR）；</li>
 *   <li>@Order(3) 其余 —— online：permitAll + 可选 token（MEMBER）。</li>
 * </ol>
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

    /** 登录失败锁定：internal 账密登录在 service 层调用（记录/判定/清零） */
    @Bean
    @ConditionalOnMissingBean(com.scaffold.starter.security.policy.LoginFailureGuard.class)
    public com.scaffold.starter.security.policy.LoginFailureGuard loginFailureGuard(
            StringRedisTemplate redisTemplate, SecurityProperties properties) {
        return new com.scaffold.starter.security.policy.LoginFailureGuard(
                redisTemplate, properties.getPasswordPolicy());
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
}
