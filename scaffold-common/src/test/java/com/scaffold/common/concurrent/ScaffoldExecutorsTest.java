package com.scaffold.common.concurrent;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ScaffoldExecutors 透传语义测试：验证 TTL 上下文随任务提交正确传递与清理。
 *
 * @author lianghang
 */
class ScaffoldExecutorsTest {

    private final TransmittableThreadLocal<String> holder = new TransmittableThreadLocal<>();

    @Test
    void decoratedPoolTransmitsSnapshotAndReplays() throws Exception {
        ExecutorService rawPool = Executors.newFixedThreadPool(1);
        ExecutorService pool = ScaffoldExecutors.decorate(rawPool);

        holder.set("tenant-A");
        AtomicReference<String> seen1 = new AtomicReference<>();
        AtomicReference<String> seen2 = new AtomicReference<>();
        // 同一线程复用跑两个任务：各自捕获提交时刻的快照，互不串号
        pool.submit(() -> seen1.set(holder.get())).get(5, TimeUnit.SECONDS);
        holder.set("tenant-B");
        pool.submit(() -> seen2.set(holder.get())).get(5, TimeUnit.SECONDS);

        assertEquals("tenant-A", seen1.get());
        assertEquals("tenant-B", seen2.get());
        assertTrue(ScaffoldExecutors.isTtlWrapped(pool));

        holder.remove();
        rawPool.shutdownNow();
    }

    @Test
    void rawPoolIsUnreliable() throws Exception {
        ExecutorService rawPool = Executors.newFixedThreadPool(1);
        // 预热：先造出复用线程（新建线程会走 ITL 继承侥幸拿到值，复用线程则不会——这正是不确定性）
        rawPool.submit(() -> { }).get(5, TimeUnit.SECONDS);

        holder.set("tenant-C");
        AtomicReference<String> seen = new AtomicReference<>("sentinel");
        rawPool.submit(() -> seen.set(holder.get())).get(5, TimeUnit.SECONDS);

        // 未包装的裸池 + 复用线程：读不到提交线程的值（这正是必须经 ScaffoldExecutors 包装的原因）
        assertNull(seen.get());

        holder.remove();
        rawPool.shutdownNow();
    }
}
