package com.scaffold.starter.data.jpa.datasource;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 当前线程的数据源 key 持有器，{@link DynamicRoutingDataSource} 据此决定路由目标。
 *
 * <p>以栈（Deque）保存 key：方法与类可同时标注 {@link TargetDataSource}，
 * 切面按「进入 push / 退出 pop」配对操作，嵌套调用各自恢复上一层的数据源，
 * 最外层退出后栈清空、路由回落默认主库。</p>
 *
 * <p>TTL 底座：经 ScaffoldExecutors 包装的线程池 / @Async 装饰器中照样取得到
 * 「提交那一刻」的数据源 key，异步场景路由意图不丢失。</p>
 *
 * @author lianghang
 */
public final class DataSourceContextHolder {

    private static final TransmittableThreadLocal<Deque<String>> HOLDER =
            TransmittableThreadLocal.withInitial(ArrayDeque::new);

    private DataSourceContextHolder() {
    }

    /**
     * 压入数据源 key（进入标注方法时调用）。
     */
    public static void push(String key) {
        HOLDER.get().push(key);
    }

    /**
     * 弹出栈顶 key（退出标注方法时调用），恢复到外层的数据源。
     */
    public static void pop() {
        Deque<String> stack = HOLDER.get();
        stack.pop();
        if (stack.isEmpty()) {
            HOLDER.remove();
        }
    }

    /**
     * 读取当前生效的数据源 key；栈空返回 null，由路由器回落默认主库。
     */
    public static String peek() {
        return HOLDER.get().peek();
    }
}
