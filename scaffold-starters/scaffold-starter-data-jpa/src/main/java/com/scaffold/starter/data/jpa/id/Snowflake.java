package com.scaffold.starter.data.jpa.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * 雪花 ID 生成器（64 bit：1 符号 + 41 时间戳 + 5 机房 + 5 机器 + 12 序列）。
 * workerId/datacenterId 来自配置（scaffold.data.worker-id / scaffold.data.datacenter-id），
 * 多实例部署时必须保证各节点取值不同。
 */
public class Snowflake {

    private static final Logger log = LoggerFactory.getLogger(Snowflake.class);

    private static final long EPOCH = 1577808000000L; // 2020-01-01
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public Snowflake(@Value("${scaffold.data.worker-id:1}") long workerId,
                     @Value("${scaffold.data.datacenter-id:1}") long datacenterId) {
        long maxWorkerId = (1L << WORKER_ID_BITS) - 1;
        long maxDatacenterId = (1L << DATACENTER_ID_BITS) - 1;
        if (workerId < 0 || workerId > maxWorkerId || datacenterId < 0 || datacenterId > maxDatacenterId) {
            throw new IllegalArgumentException(
                    "workerId/datacenterId 取值必须在 0~" + maxWorkerId + " 之间: "
                            + workerId + "/" + datacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        log.info("Snowflake 初始化: workerId={}, datacenterId={}", workerId, datacenterId);
    }

    /** 生成全局唯一 Long ID（线程安全） */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            // 时钟回拨：等待追平，超过 5ms 直接报错
            long offset = lastTimestamp - timestamp;
            if (offset > 5) {
                throw new IllegalStateException("时钟回拨过大: " + offset + "ms");
            }
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long ts = System.currentTimeMillis();
        while (ts <= lastTimestamp) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }
}
