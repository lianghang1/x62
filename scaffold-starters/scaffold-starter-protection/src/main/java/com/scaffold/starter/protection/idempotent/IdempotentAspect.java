package com.scaffold.starter.protection.idempotent;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 接口幂等切面，配合 {@link Idempotent} 注解工作。
 *
 * <p>幂等键经 setIfAbsent 占位，窗口内重复请求直接拒绝——占位即提交登记，
 * 首个到达者胜出，后续者一律 1409。</p>
 *
 * @author lianghang
 */
@Aspect
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    private final StringRedisTemplate redisTemplate;

    public IdempotentAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Before("@annotation(idempotent)")
    public void check(JoinPoint joinPoint, Idempotent idempotent) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        String identity = request.getHeader("X-Account-Id");
        if (identity == null) {
            identity = request.getRemoteAddr();
        }
        String argsDigest = md5(Arrays.stream(joinPoint.getArgs())
                .map(a -> a == null ? "null" : String.valueOf(a.hashCode()))
                .collect(Collectors.joining("|")));
        String key = "scaffold:idempotent:" + request.getMethod() + ":"
                + request.getRequestURI() + ":" + identity + ":" + argsDigest;

        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(idempotent.windowSeconds()));
        if (first == null || !first) {
            log.warn("[idempotent] 重复提交拦截: key={}", key);
            throw BizException.of(GlobalErrorCode.CONFLICT, "请勿重复提交");
        }
    }

    private String md5(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }
}
