package com.scaffold.starter.web.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 访问日志切面：对所有 @RestController 的调用记录 URI / 参数 / 耗时（traceId 已在 MDC）。
 * 参数序列化失败不影响业务；文件与大对象做截断处理。
 */
@Aspect
public class AccessLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AccessLogAspect.class);
    private static final int MAX_ARGS_LENGTH = 500;

    private final ObjectMapper objectMapper;

    public AccessLogAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("[access] {} {} args={} cost={}ms",
                    httpMethod(), uri(), serializeArgs(pjp.getArgs()),
                    System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            log.info("[access] {} {} args={} cost={}ms error={}",
                    httpMethod(), uri(), serializeArgs(pjp.getArgs()),
                    System.currentTimeMillis() - start, t.getClass().getSimpleName());
            throw t;
        }
    }

    private String httpMethod() {
        HttpServletRequest request = currentRequest();
        return request == null ? "-" : request.getMethod();
    }

    private String uri() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "-";
        }
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        try {
            String json = objectMapper.writeValueAsString(
                    Arrays.stream(args).map(this::simplify).collect(Collectors.toList()));
            json = maskSensitive(json);
            return json.length() > MAX_ARGS_LENGTH ? json.substring(0, MAX_ARGS_LENGTH) + "..." : json;
        } catch (Exception e) {
            // 参数里可能有不可序列化对象（流/大对象），降级为类型列表
            return Arrays.stream(args).map(a -> a == null ? "null" : a.getClass().getSimpleName())
                    .collect(Collectors.toList()).toString();
        }
    }

    /** 敏感字段脱敏（password/token/secret），防止访问日志泄漏凭据 */
    private String maskSensitive(String json) {
        return json
                .replaceAll("(\"(password|passwd|secret|appSecret|token|accessToken)\"\\s*:\\s*\")[^\"]*(\")", "$1***$3");
    }

    private Object simplify(Object arg) {
        if (arg instanceof MultipartFile) {
            return "file:" + ((MultipartFile) arg).getOriginalFilename();
        }
        if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
            return arg.getClass().getSimpleName();
        }
        return arg;
    }
}
