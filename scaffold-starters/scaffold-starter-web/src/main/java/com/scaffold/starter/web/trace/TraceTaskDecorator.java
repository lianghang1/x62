package com.scaffold.starter.web.trace;

import com.alibaba.ttl.TtlRunnable;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 异步任务装饰器，把提交线程的 MDC（traceId）复制到执行线程，并叠加阿里 TTL 透传。
 *
 * <p>两层各管一摊：MDC 由本类手工快照/恢复/清理（保证异步日志与触发请求串联）；
 * LoginContext、审计人等 TransmittableThreadLocal 上下文经 {@link TtlRunnable}
 * 透传（提交时快照、执行线程还原），@Async 中取身份不再为 null。</p>
 *
 * <p>提交时快照上下文、执行时恢复、finally 清除，保证线程池复用下不串链路不串号。</p>
 *
 * @author lianghang
 */
public class TraceTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // TTL 透传在最内层：快照于提交线程，执行线程自动还原
        final Runnable ttlWrapped = TtlRunnable.get(runnable);
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                ttlWrapped.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
