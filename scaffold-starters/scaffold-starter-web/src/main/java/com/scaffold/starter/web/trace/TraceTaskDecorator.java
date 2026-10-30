package com.scaffold.starter.web.trace;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 异步任务装饰器，把提交线程的 MDC（traceId 等）复制到执行线程并在执行完清理。
 *
 * <p>提交时快照上下文、执行时恢复、finally 清除，保证线程池复用下异步日志仍能
 * 与触发请求串联且不串链路。LoginContext 的传递由 TTL 底座负责（starter-security），
 * 两者互补、各管一层。</p>
 *
 * @author lianghang
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
