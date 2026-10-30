package com.scaffold.starter.protection.ratelimit;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 接口限流切面，配合 {@link RateLimit} 注解工作。
 *
 * <p>计数与过期在同一 Lua 脚本内原子完成，杜绝「先 INCR 后 EXPIRE」的竞态；
 * 无法确定限流维度（如无请求上下文）时放行以避免误伤内部调用，超限抛 1429 业务异常（HTTP 429）。</p>
 *
 * @author lianghang
 */
@Aspect
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /** KEYS[1]=计数 key；ARGV[1]=窗口秒；返回值 1=放行 0=超限（脚本内同步设置 EXPIRE） */
    private static final String LUA = ""
            + "local current = redis.call('INCR', KEYS[1]) "
            + "if current == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end "
            + "return current";

    private static final DefaultRedisScript<Long> SCRIPT =
            new DefaultRedisScript<>(LUA, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Before("@annotation(rateLimit)")
    public void check(JoinPoint joinPoint, RateLimit rateLimit) {
        String dimensionValue = resolveDimensionValue(rateLimit.dimension());
        if (dimensionValue == null) {
            return; // 无法确定维度（如无请求上下文）时放行，避免误伤内部调用
        }
        String key = "scaffold:ratelimit:" + rateLimit.dimension() + ":"
                + (rateLimit.key().isEmpty() ? "" : rateLimit.key() + ":") + dimensionValue;
        List<String> keys = Arrays.asList(key);
        Long count = redisTemplate.execute(SCRIPT, keys, String.valueOf(rateLimit.windowSeconds()));
        if (count != null && count > rateLimit.limit()) {
            log.warn("[ratelimit] 触发限流: key={} count={} limit={}", key, count, rateLimit.limit());
            throw BizException.of(GlobalErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private String resolveDimensionValue(String dimension) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        switch (dimension) {
            case "ip":
                return clientIp(request);
            case "account":
                String accountId = request.getHeader("X-Account-Id");
                return accountId != null ? accountId : clientIp(request);
            case "uri":
            default:
                return request.getMethod() + ":" + request.getRequestURI();
        }
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
