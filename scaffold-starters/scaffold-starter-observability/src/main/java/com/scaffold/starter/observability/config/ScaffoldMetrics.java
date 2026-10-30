package com.scaffold.starter.observability.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * 框架指标门面：以 {@code scaffold.*} 命名空间收口内建与业务自定义指标的统一入口。
 *
 * <p>替代散落的 {@code MeterRegistry} 直用：计数与耗时两个高频动作一行调用，
 * 标签以 k1,v1,k2,v2 平铺传入（奇数位为 key，允许为空表示无标签）。命名规约
 * 遵循 Micrometer 点分小写：{@code scaffold.<模块>.<指标>}，Prometheus 抓取后
 * 自动转下划线（scaffold_job_executions_total）。</p>
 *
 * <p>门面不可用时（observability 关闭或无 registry）业务方埋点调用方应做空值
 * 防御——框架内建埋点走各 starter 的 optional registry 判空，不经由本类。</p>
 *
 * @author lianghang
 */
public class ScaffoldMetrics {

    /** 框架指标统一命名前缀 */
    public static final String PREFIX = "scaffold";

    private final MeterRegistry registry;

    public ScaffoldMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** 计数 +1：increment("scaffold.order.created", "channel", "app")。 */
    public void increment(String name, String... tagKeyValuePairs) {
        registry.counter(name, tags(tagKeyValuePairs)).increment();
    }

    /** 记录一次耗时（毫秒）：Timer 自动统计 count/sum/max 与百分位。 */
    public void record(String name, long costMillis, String... tagKeyValuePairs) {
        Timer.builder(name).tags(tags(tagKeyValuePairs)).register(registry)
                .record(costMillis, TimeUnit.MILLISECONDS);
    }

    /** 直取 MeterRegistry，Micrometer 高级用法（分布直方图/自定义 Meter）兜底入口。 */
    public MeterRegistry registry() {
        return registry;
    }

    private Iterable<Tag> tags(String[] pairs) {
        java.util.List<Tag> list = new java.util.ArrayList<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            list.add(Tag.of(pairs[i], pairs[i + 1]));
        }
        return list;
    }
}
