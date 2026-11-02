package com.scaffold.cemlink.client.model;

/**
 * 订单同步状态回传（对齐原契约 ServiceOrderUpdateData）
 *
 * @author lianghang
 */
public class ServiceOrderUpdateData {

    /** server 订单ID */
    private String id;
    /** 订单号 */
    private String orderNo;
    /** 微商订单类型 */
    private String orderType;

    public ServiceOrderUpdateData() {
    }

    public ServiceOrderUpdateData(String id, String orderNo, String orderType) {
        this.id = id;
        this.orderNo = orderNo;
        this.orderType = orderType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
}
