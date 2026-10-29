package com.scaffold.server.demo.order.entity;

import com.scaffold.starter.data.jpa.entity.VersionedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 示例订单（演示实体规约：VersionedEntity 乐观锁 + 状态走字典）。
 */
@Entity
@Table(name = "demo_order", indexes = {
        @Index(name = "idx_demo_order_owner", columnList = "owner_account_id"),
        @Index(name = "idx_demo_order_no", columnList = "order_no", unique = true)
})
public class DemoOrder extends VersionedEntity {

    @Column(name = "order_no", length = 32, nullable = false)
    private String orderNo;

    @Column(name = "title", length = 128, nullable = false)
    private String title;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    /** 订单状态：字典类型 order_status（CREATED/PAID/CANCELLED...） */
    @Column(name = "status", length = 32, nullable = false)
    private String status = "CREATED";

    /** 归属人：C 端=会员账户，openapi=APP 账户，internal=操作员账户（统一账户体系） */
    @Column(name = "owner_account_id", nullable = false)
    private Long ownerAccountId;

    /** openapi 三方侧终端用户标识（备查，不进主模型——见计划文档身份模型） */
    @Column(name = "external_user_no", length = 128)
    private String externalUserNo;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getOwnerAccountId() {
        return ownerAccountId;
    }

    public void setOwnerAccountId(Long ownerAccountId) {
        this.ownerAccountId = ownerAccountId;
    }

    public String getExternalUserNo() {
        return externalUserNo;
    }

    public void setExternalUserNo(String externalUserNo) {
        this.externalUserNo = externalUserNo;
    }
}
