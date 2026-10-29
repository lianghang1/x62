package com.scaffold.starter.data.jpa.config;

/**
 * 数据层配置项（前缀 scaffold.data）。
 * workerId/datacenterId 在多实例部署时必须各节点不同（可由环境变量注入）。
 */
public class SnowflakeProperties {

    /** 机器 ID（0~31） */
    private long workerId = 1;

    /** 机房 ID（0~31） */
    private long datacenterId = 1;

    public long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(long workerId) {
        this.workerId = workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(long datacenterId) {
        this.datacenterId = datacenterId;
    }
}
