package com.scaffold.module.cemlink.client.model;

import java.util.List;

/**
 * 服务项目上行数据（对齐 server 契约 TombServiceItemData）
 *
 * @author lianghang
 */
public class TombServiceItemData {

    /** 服务项目ID */
    private String id;
    /** 服务名称 */
    private String name;
    /** 服务售价，单位：分 */
    private Long price;
    /** 数量 */
    private Integer qty;
    /** 序号 */
    private Integer orderNo;
    /** 服务状态，1：可售【默认】；0：不可售 */
    private String status;
    /** 对应微商订单类型 */
    private String orderType;
    /** 服务编码 */
    private String code;
    /** 服务图片集 */
    private List<String> images;

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

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
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

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
