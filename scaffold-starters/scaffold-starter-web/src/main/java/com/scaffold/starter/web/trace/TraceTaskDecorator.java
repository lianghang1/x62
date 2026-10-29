package com.scaffold.starter.web.trace;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 异步任务装饰器：把提交线程的 MDC（traceId 等）复制到执行线程，执行完清理。
 * LoginContext 的传递由 TTL 底座负责（starter-security），两者互补。
 */
public class TraceTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
