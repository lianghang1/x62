package com.scaffold.starter.web.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * {@code @Async} void 方法未捕获异常的统一兜底。
 *
 * <p>默认行为只留一行 Spring 日志，调用方无感知；这里补齐方法签名与入参记录，
 * 定位「异步任务静默失败」不再靠猜。返回值型异步（Future）的异常由调用方取值时获得，不经此处。</p>
 *
 * @author lianghang
 */
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AsyncExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("[async] 异步任务未捕获异常: method={}#{}({}), params={}",
                method.getDeclaringClass().getSimpleName(), method.getName(),
                method.getParameterCount() == 0 ? "" : Arrays.toString(params),
                ex.getMessage(), ex);
    }
}
