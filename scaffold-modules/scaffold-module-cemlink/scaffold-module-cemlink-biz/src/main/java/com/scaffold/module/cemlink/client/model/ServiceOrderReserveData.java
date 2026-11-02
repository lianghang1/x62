package com.scaffold.module.cemlink.client.model;

/**
 * 订单服务明细（对齐原契约 ServiceOrderReserveData）
 *
 * @author lianghang
 */
public class ServiceOrderReserveData {

    /** 预订服务类型 */
    private String type;
    /** 服务ID */
    private String serviceId;
    /** 服务名称 */
    private String serviceName;
    /** 单价，单位：分 */
    private Long price;
    /** 数量 */
    private Integer qty;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
}
