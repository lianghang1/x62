package com.scaffold.common.event;

import com.scaffold.common.util.SpringContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事件发布门面：进程内解耦主链路的标准出口。
 *
 * <p>委托 {@link SpringContextHolder#publishEvent}：{@link #publish} 立即发布，
 * 供无事务或与事务无关的通知；{@link #publishAfterCommit} 事务提交后才发布，
 * 供「先落库、再通知」类副动作（日志、缓存失效、推送），回滚即不发——
 * 消费方用 {@code @EventListener}（配 {@code @Async}）承接，见 AuditLogListener。</p>
 *
 * <p>规约：事件对象设计为不可变 POJO，生产方在请求线程内取齐上下文
 * （IP、操作人等）再发布——异步消费线程里 RequestContextHolder 已不可用。</p>
 *
 * @author lianghang
 */
public final class EventOps {

    private EventOps() {
    }

    /** 立即发布事件（不管当前是否有事务）。 */
    public static void publish(Object event) {
        SpringContextHolder.publishEvent(event);
    }

    /**
     * 事务提交后发布；无活动事务时退化为立即发布。
     *
     * <p>回滚场景不发布——消费方承诺的副动作只对成功落库生效。</p>
     */
    public static void publishAfterCommit(Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(event);
                }
            });
        } else {
            publish(event);
        }
    }
}
