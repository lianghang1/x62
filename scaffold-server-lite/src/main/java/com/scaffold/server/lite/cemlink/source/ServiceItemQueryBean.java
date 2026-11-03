package com.scaffold.server.lite.cemlink.source;

import java.util.Date;

/**
 * 服务项目查询 Bean（对应原采集程序 TombCodeServiceQueryBean）
 *
 * @author lianghang
 */
public class ServiceItemQueryBean {

    /** 服务项目ID */
    private String id;
    /** 服务名称 */
    private String name;
    /** 服务售价，单位：分 */
    private Long price;
    /** 数量（SQL 常量 1） */
    private Integer qty;
    /** 序号 */
    private Integer orderNo;
    /** 服务状态（is_validate） */
    private String status;
    /** 单据类型 */
    private String billType;
    /** 单据子类型 */
    private String billSubType;
    /** 修改时间（水位键） */
    private Date modifyDate;
    /** 微商订单类型（按单据子类型映射，非数据库列） */
    private String orderTypeNo;

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

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public String getBillSubType() {
        return billSubType;
    }

    public void setBillSubType(String billSubType) {
        this.billSubType = billSubType;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    public String getOrderTypeNo() {
        return orderTypeNo;
    }

    public void setOrderTypeNo(String orderTypeNo) {
        this.orderTypeNo = orderTypeNo;
    }
}
