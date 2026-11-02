package com.scaffold.cemlink.client.model;

/**
 * 微商服务项目下行数据（对齐 server 契约 ServiceItemData）
 *
 * @author lianghang
 */
public class ServiceItemData {

    /** server 服务项目ID */
    private String id;
    /** 服务名称 */
    private String name;
    /** 服务售价，单位：分 */
    private Long price;
    /** 序号 */
    private Integer orderNo;
    /** 服务状态，1：可售【默认】；0：不可售 */
    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
