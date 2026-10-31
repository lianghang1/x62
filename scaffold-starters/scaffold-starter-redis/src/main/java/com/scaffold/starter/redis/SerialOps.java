package com.scaffold.starter.redis;

import com.scaffold.common.constant.RedisKeys;
import com.scaffold.common.util.DateUtils;

import java.time.Duration;
import java.time.LocalDate;

/**
 * 业务单号生成器：规则模板 + 日期段 + Redis 原子序列。
 *
 * <p>与 {@code IdOps}（雪花）互补——雪花负责趋势递增主键（给机器看），
 * 本类负责可读业务单号（给人看），形如 {@code ORD20261031-0001}。</p>
 *
 * <p>实现要点：</p>
 * <ul>
 *   <li>序列 key 内嵌日期段（{@code scaffold:serial:ORD:20261031}），
 *       跨日天然重置归零，无需定时清理；</li>
 *   <li>自增走 {@link RedisOps#increment}，多实例并发取号不重号；</li>
 *   <li>首个序号（incr 返回 1）时补 TTL——key 自带日期所以 TTL 只是兜底回收，
 *       取 48h 覆盖跨日时钟漂移即可；</li>
 *   <li>序列超出指定位宽时自然变长（不截断不回绕），不丢号。</li>
 * </ul>
 *
 * <p>规约：prefix 用业务语义大写短码（ORD/PAY/REFUND…），全局唯一，
 * 它直接决定 Redis key，混用会导致两类业务共享序列。</p>
 *
 * @author lianghang
 */
public class SerialOps {

    /** 序列号 key 兜底 TTL：key 含日期段，过期只为回收，取 48h 覆盖跨日时钟漂移 */
    private static final Duration KEY_TTL = Duration.ofHours(48);

    /** 默认序列位宽 */
    private static final int DEFAULT_SEQ_LENGTH = 4;

    private final RedisOps redisOps;

    public SerialOps(RedisOps redisOps) {
        this.redisOps = redisOps;
    }

    /** 取单号：prefix + yyyyMMdd + '-' + 4 位序列，例 {@code ORD20261031-0001} */
    public String next(String prefix) {
        return next(prefix, DEFAULT_SEQ_LENGTH);
    }

    /**
     * 取单号（自定义序列位宽）。
     *
     * @param prefix    业务短码（如 ORD/PAY），作 Redis key 一部分，不允许空白
     * @param seqLength 序列号位宽（不足左补 0，超出自然变长）
     * @return 形如 {@code PAY20261031-000001} 的单号
     */
    public String next(String prefix, int seqLength) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("SerialOps: prefix 不能为空");
        }
        if (seqLength < 1 || seqLength > 18) {
            throw new IllegalArgumentException("SerialOps: seqLength 应在 1~18, 当前=" + seqLength);
        }
        String dateSegment = DateUtils.formatDateCompact(LocalDate.now());
        String key = RedisKeys.SERIAL + prefix + ":" + dateSegment;
        long seq = redisOps.increment(key, 1);
        if (seq == 1L) {
            // 仅首个取号者补 TTL；expire 失败不影响发号（key 自带日期，漏删只是占用内存）
            redisOps.expire(key, KEY_TTL);
        }
        return prefix + dateSegment + "-" + String.format("%0" + seqLength + "d", seq);
    }
}
