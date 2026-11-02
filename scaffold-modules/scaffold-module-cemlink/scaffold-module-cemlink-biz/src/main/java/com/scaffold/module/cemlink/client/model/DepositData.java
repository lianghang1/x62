package com.scaffold.module.cemlink.client.model;

/**
 * 骨灰寄存上行数据（对齐 server 契约 DepositData）
 *
 * @author lianghang
 */
public class DepositData {

    /** 业务编号 */
    private String bizNo;
    /** 寄存格位 */
    private String grid;
    /** 寄存状态 */
    private String state;
    /** 使用人姓名 */
    private String userName;
    /** 使用人数量 */
    private String userNum;
    /** 寄存数量 */
    private Integer depositTimes;
    /** 收费类别 */
    private String depositCharge;
    /** 开始时间 */
    private String startDate;
    /** 结束时间 */
    private String endDate;
    /** 寄存格位面积 */
    private String gridArea;
    /** 寄存格位层数 */
    private String gridLayer;
    /** 承办人手机号码 */
    private String directorPhone;
    /** 承办人姓名 */
    private String directorName;
    /** 格位单价，单位：分 */
    private Long price;
    /** 管理费，单位：分 */
    private Long managePrice;
    /** 是否可以续存 */
    private Boolean renewable;
    /** 二维码 */
    private String qrCode;

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getGrid() {
        return grid;
    }

    public void setGrid(String grid) {
        this.grid = grid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }

    public Integer getDepositTimes() {
        return depositTimes;
    }

    public void setDepositTimes(Integer depositTimes) {
        this.depositTimes = depositTimes;
    }

    public String getDepositCharge() {
        return depositCharge;
    }

    public void setDepositCharge(String depositCharge) {
        this.depositCharge = depositCharge;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getGridArea() {
        return gridArea;
    }

    public void setGridArea(String gridArea) {
        this.gridArea = gridArea;
    }

    public String getGridLayer() {
        return gridLayer;
    }

    public void setGridLayer(String gridLayer) {
        this.gridLayer = gridLayer;
    }

    public String getDirectorPhone() {
        return directorPhone;
    }

    public void setDirectorPhone(String directorPhone) {
        this.directorPhone = directorPhone;
    }

    public String getDirectorName() {
        return directorName;
    }

    public void setDirectorName(String directorName) {
        this.directorName = directorName;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Long getManagePrice() {
        return managePrice;
    }

    public void setManagePrice(Long managePrice) {
        this.managePrice = managePrice;
    }

    public Boolean getRenewable() {
        return renewable;
    }

    public void setRenewable(Boolean renewable) {
        this.renewable = renewable;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
}
