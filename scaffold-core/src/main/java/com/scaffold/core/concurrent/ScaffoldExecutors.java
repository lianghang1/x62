package com.scaffold.core.concurrent;

import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 线程池上下文透传的统一入口，包装底座为阿里 TransmittableThreadLocal（TTL）。
 *
 * <p>脚手架的线程级上下文（LoginContext 登录身份、ScaffoldAuditorHolder 审计人、
 * MDC traceId）全部承载在 TransmittableThreadLocal 上：提交任务时快照、
 * 执行线程还原、执行完自动清理——线程池复用不串号，嵌套提交逐层透传。</p>
 *
 * <p>业务侧规约：自建线程池一律经 {@link #decorate(ExecutorService)} 包装后再用；
 * Spring 的 ThreadPoolTaskExecutor（@Async 默认池）由各 starter 的 TaskDecorator 调
 * {@link #wrap(Runnable)} 组装；禁止把上下文当方法参数在业务代码里手工层层传递。</p>
 *
 * @author lianghang
 */
public final class ScaffoldExecutors {

    private ScaffoldExecutors() {
    }

    /** 包装 Executor：提交时的 TTL 快照在执行线程可见（TtlExecutors 语义） */
    public static Executor decorate(Executor executor) {
        return TtlExecutors.getTtlExecutor(executor);
    }

    /** 包装 ExecutorService：同 {@link #decorate(Executor)} */
    public static ExecutorService decorate(ExecutorService executorService) {
        return TtlExecutors.getTtlExecutorService(executorService);
    }

    /** 包装 ScheduledExecutorService：定时任务同样透传 TTL 上下文 */
    public static ScheduledExecutorService decorate(ScheduledExecutorService scheduledExecutorService) {
        return TtlExecutors.getTtlScheduledExecutorService(scheduledExecutorService);
    }

    /** 裸 Runnable 的 TTL 包装（Spring TaskDecorator 场景由各 starter 自行组装） */
    public static Runnable wrap(Runnable runnable) {
        return TtlRunnable.get(runnable);
    }

    /** 是否为已包装的 TTL 透传通道（幂等包装防呆用） */
    public static boolean isTtlWrapped(Executor executor) {
        return TtlExecutors.isTtlWrapper(executor);
    }

    /** 取当前线程绑定的 TTL 值（测试与框架内部用，业务走各自上下文门面） */
    public static <T> T get(TransmittableThreadLocal<T> holder) {
        return holder.get();
    }
}
