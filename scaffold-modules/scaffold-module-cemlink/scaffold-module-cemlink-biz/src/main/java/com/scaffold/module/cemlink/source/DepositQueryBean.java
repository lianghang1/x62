package com.scaffold.module.cemlink.source;

import java.util.Date;

/**
 * 寄存业务查询 Bean（对应原采集程序 AshesDepositQueryBean）
 *
 * @author lianghang
 */
public class DepositQueryBean {

    /** 寄存业务ID */
    private String id;
    /** 业务编号 */
    private String bizNo;
    /** 寄存格位 */
    private String grid;
    /** 承办人姓名 */
    private String directorName;
    /** 承办人手机号码 */
    private String directorPhone;
    /** 使用人姓名 */
    private String userName;
    /** 业务状态 */
    private String state;
    /** 寄存年限 */
    private Integer depositTimes;
    /** 寄存费 */
    private Double depositFee;
    /** 开始时间 */
    private String startDate;
    /** 结束时间 */
    private String endDate;
    /** 二维码 */
    private String qrCode;
    /** 使用人数量（格位规格） */
    private String userNum;
    /** 格位单价 */
    private Double gridFee;
    /** 寄存格位面积 */
    private String gridArea;
    /** 寄存格位层数 */
    private String gridLayer;
    /** 是否有效 */
    private String isValidate;
    /** 格位所属墓区是否在线 */
    private String isOnLine;
    /** 修改时间（水位键） */
    private Date modifyDate;
    /** 格位单价，单位：分（计算结果，非数据库列） */
    private Long price;
    /** 管理费，单位：分（计算结果，非数据库列） */
    private Long managePrice;
    /** 收费类别（计算结果，非数据库列） */
    private String depositCharge;
    /** 是否可以续存（计算结果，非数据库列） */
    private Boolean renewable;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getDirectorName() {
        return directorName;
    }

    public void setDirectorName(String directorName) {
        this.directorName = directorName;
    }

    public String getDirectorPhone() {
        return directorPhone;
    }

    public void setDirectorPhone(String directorPhone) {
        this.directorPhone = directorPhone;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getDepositTimes() {
        return depositTimes;
    }

    public void setDepositTimes(Integer depositTimes) {
        this.depositTimes = depositTimes;
    }

    public Double getDepositFee() {
        return depositFee;
    }

    public void setDepositFee(Double depositFee) {
        this.depositFee = depositFee;
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

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }

    public Double getGridFee() {
        return gridFee;
    }

    public void setGridFee(Double gridFee) {
        this.gridFee = gridFee;
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

    public String getIsValidate() {
        return isValidate;
    }

    public void setIsValidate(String isValidate) {
        this.isValidate = isValidate;
    }

    public String getIsOnLine() {
        return isOnLine;
    }

    public void setIsOnLine(String isOnLine) {
        this.isOnLine = isOnLine;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
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

    public String getDepositCharge() {
        return depositCharge;
    }

    public void setDepositCharge(String depositCharge) {
        this.depositCharge = depositCharge;
    }

    public Boolean getRenewable() {
        return renewable;
    }

    public void setRenewable(Boolean renewable) {
        this.renewable = renewable;
    }
}
