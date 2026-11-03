package com.scaffold.server.lite.cemlink.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * 采集水位线（主库业务表）：按 (域, 业务ID) 记录已推送版本，重启不丢
 *
 * @author lianghang
 */
@Entity
@Table(name = "cemlink_watermark", uniqueConstraints = @UniqueConstraint(columnNames = {"domain", "biz_id"}),
        indexes = @Index(columnList = "domain, biz_id"))
public class WatermarkEntity extends BaseEntity {

    /** 业务域（tomb/block/service-item/deposit） */
    @Column(name = "domain", nullable = false, length = 32)
    private String domain;

    /** 业务ID（墓穴ID/工单号/订单号…） */
    @Column(name = "biz_id", nullable = false, length = 64)
    private String bizId;

    /** 已推送版本（时间戳毫秒或数值版本） */
    @Column(name = "version", nullable = false)
    private Long version;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
