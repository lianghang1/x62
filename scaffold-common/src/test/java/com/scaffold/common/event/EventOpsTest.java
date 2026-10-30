package com.scaffold.common.event;

import com.scaffold.common.util.SpringContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EventOps 测试：发布/监听、事务提交后发布的时序语义。
 *
 * @author lianghang
 */
class EventOpsTest {

    /** 测试事件 */
    static class PingEvent {
        final String name;

        PingEvent(String name) {
            this.name = name;
        }
    }

    /** 监听器：收到的名字记入静态列表，跨容器实例可断言 */
    static class PingListener {

        final List<String> received = new ArrayList<>();

        @EventListener
        void onPing(PingEvent event) {
            received.add(event.name);
        }
    }

    private AnnotationConfigApplicationContext context;
    private final PingListener listener = new PingListener();

    @BeforeEach
    void setUp() {
        // AnnotationConfigApplicationContext 自身即 ApplicationEventPublisher，
        // 以单例注册后 EventOps 经 SpringContextHolder 按类型取到的就是容器
        context = new AnnotationConfigApplicationContext();
        context.registerBean("pingListener", PingListener.class, () -> listener);
        context.register(SpringContextHolder.class);
        context.getBeanFactory().registerSingleton("applicationEventPublisher", context);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        context.close();
    }

    @Test
    void publishDeliversImmediately() {
        EventOps.publish(new PingEvent("a"));
        assertEquals(1, listener.received.size());
        assertEquals("a", listener.received.get(0));
    }

    @Test
    void publishAfterCommitWithoutTxIsImmediate() {
        EventOps.publishAfterCommit(new PingEvent("no-tx"));
        assertEquals(1, listener.received.size());
    }

    @Test
    void publishAfterCommitDefersUntilCommit() throws Exception {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        EventOps.publishAfterCommit(new PingEvent("deferred"));
        // 提交前不可达
        assertTrue(listener.received.isEmpty());
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        assertEquals(1, listener.received.size());
        assertEquals("deferred", listener.received.get(0));
    }
}
