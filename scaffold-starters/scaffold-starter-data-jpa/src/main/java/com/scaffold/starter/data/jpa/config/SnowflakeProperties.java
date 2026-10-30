package com.scaffold.starter.data.jpa.config;

/**
 * 数据层配置属性，绑定前缀 {@code scaffold.data}，承载雪花 ID 的节点标识。
 *
 * <p>{@code workerId/datacenterId} 在多实例部署时必须各节点不同，否则不同实例
 * 会生成重复 ID——建议通过环境变量按实例注入。</p>
 *
 * @author lianghang
 */
public class SnowflakeProperties {

    /** 机器 ID（0~31）。 */
    private long workerId = 1;

    /** 机房 ID（0~31）。 */
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
