package com.scaffold.module.cemlink.api.payload;

import java.io.Serializable;
import java.util.Date;

/**
 * 传输墓穴信息：与 ERP 侧 contract-base 的 TransportTomb 字段 1:1 对齐
 * （33 字段，字段名即 JSON key，两端一致即可互通）。
 *
 * <p>来源：墓园本地库 t_sfi_tomb 关联查询（旧采集程序 TombBaseDataCollectionServiceImpl 平移）。</p>
 *
 * @author lianghang
 */
public class TransportTomb implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公墓系统购墓ID */
    private String tombId;
    /** 公墓系统 墓穴所属墓区ID */
    private String blockId;
    /** 所在墓区全称 */
    private String blockFullName;
    /** 墓穴编号 */
    private String tombNo;
    /** 墓穴位置 */
    private String tombPath;
    /** 墓穴数 */
    private int tombs;
    /** 墓型 */
    private String typeCode;
    /** 葬式 */
    private String buryStyle;
    /** 净墓面积 */
    private String netArea;
    /** 占地面积 */
    private String floorArea;
    /** 购买时间 */
    private Date buyTime;
    /** 结算状态 */
    private String payState;
    /** 承办人手机号码 */
    private String directorPhone;
    /** 承办人姓名 */
    private String directorName;
    /** 承办人身份证号 */
    private String directorCardNo;
    /** 维护费到期时间 */
    private Date maintenanceEndTime;
    /** 实际维护费到期时间 */
    private Date relMaintenanceEndTime;
    /** 盆花租摆到期时间 */
    private Date rentingEndTime;
    /** 实际盆花租摆到期时间 */
    private Date relRentingEndTime;
    /** 现有盆花数量 */
    private Integer flowerPots;
    /** 盆花ID */
    private String flowerIds;
    /** 盆花名称 */
    private String flowerNames;
    /** 墓穴状态 */
    private String state;
    /** 维护费金额，单位：分/年 */
    private Long amountPerYear;
    /** 是否可以办理盆花租摆 */
    private Boolean rentingFlag;
    /** 是否可以办理免费描碑 */
    private Boolean freeTabletTraceFlag;
    /** 是否免维护费 */
    private Boolean freeMaintenanceFlag;
    /** 是否贴金 */
    private Boolean gildFlag;
    /** 是否可以搜索 */
    private Boolean searchFlag;
    /** 墓石一致性证书 */
    private String tombStoneCertificate;
    /** 主碑数量 */
    private Integer mainTabletNum;
    /** 绿化保养记录 */
    private String greenRecord;
    /** 保洁记录 */
    private String cleanRecord;

    public String getTombId() {
        return tombId;
    }

    public void setTombId(String tombId) {
        this.tombId = tombId;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public String getBlockFullName() {
        return blockFullName;
    }

    public void setBlockFullName(String blockFullName) {
        this.blockFullName = blockFullName;
    }

    public String getTombNo() {
        return tombNo;
    }

    public void setTombNo(String tombNo) {
        this.tombNo = tombNo;
    }

    public String getTombPath() {
        return tombPath;
    }

    public void setTombPath(String tombPath) {
        this.tombPath = tombPath;
    }

    public int getTombs() {
        return tombs;
    }

    public void setTombs(int tombs) {
        this.tombs = tombs;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getBuryStyle() {
        return buryStyle;
    }

    public void setBuryStyle(String buryStyle) {
        this.buryStyle = buryStyle;
    }

    public String getNetArea() {
        return netArea;
    }

    public void setNetArea(String netArea) {
        this.netArea = netArea;
    }

    public String getFloorArea() {
        return floorArea;
    }

    public void setFloorArea(String floorArea) {
        this.floorArea = floorArea;
    }

    public Date getBuyTime() {
        return buyTime;
    }

    public void setBuyTime(Date buyTime) {
        this.buyTime = buyTime;
    }

    public String getPayState() {
        return payState;
    }

    public void setPayState(String payState) {
        this.payState = payState;
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

    public String getDirectorCardNo() {
        return directorCardNo;
    }

    public void setDirectorCardNo(String directorCardNo) {
        this.directorCardNo = directorCardNo;
    }

    public Date getMaintenanceEndTime() {
        return maintenanceEndTime;
    }

    public void setMaintenanceEndTime(Date maintenanceEndTime) {
        this.maintenanceEndTime = maintenanceEndTime;
    }

    public Date getRelMaintenanceEndTime() {
        return relMaintenanceEndTime;
    }

    public void setRelMaintenanceEndTime(Date relMaintenanceEndTime) {
        this.relMaintenanceEndTime = relMaintenanceEndTime;
    }

    public Date getRentingEndTime() {
        return rentingEndTime;
    }

    public void setRentingEndTime(Date rentingEndTime) {
        this.rentingEndTime = rentingEndTime;
    }

    public Date getRelRentingEndTime() {
        return relRentingEndTime;
    }

    public void setRelRentingEndTime(Date relRentingEndTime) {
        this.relRentingEndTime = relRentingEndTime;
    }

    public Integer getFlowerPots() {
        return flowerPots;
    }

    public void setFlowerPots(Integer flowerPots) {
        this.flowerPots = flowerPots;
    }

    public String getFlowerIds() {
        return flowerIds;
    }

    public void setFlowerIds(String flowerIds) {
        this.flowerIds = flowerIds;
    }

    public String getFlowerNames() {
        return flowerNames;
    }

    public void setFlowerNames(String flowerNames) {
        this.flowerNames = flowerNames;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getAmountPerYear() {
        return amountPerYear;
    }

    public void setAmountPerYear(Long amountPerYear) {
        this.amountPerYear = amountPerYear;
    }

    public Boolean getRentingFlag() {
        return rentingFlag;
    }

    public void setRentingFlag(Boolean rentingFlag) {
        this.rentingFlag = rentingFlag;
    }

    public Boolean getFreeTabletTraceFlag() {
        return freeTabletTraceFlag;
    }

    public void setFreeTabletTraceFlag(Boolean freeTabletTraceFlag) {
        this.freeTabletTraceFlag = freeTabletTraceFlag;
    }

    public Boolean getFreeMaintenanceFlag() {
        return freeMaintenanceFlag;
    }

    public void setFreeMaintenanceFlag(Boolean freeMaintenanceFlag) {
        this.freeMaintenanceFlag = freeMaintenanceFlag;
    }

    public Boolean getGildFlag() {
        return gildFlag;
    }

    public void setGildFlag(Boolean gildFlag) {
        this.gildFlag = gildFlag;
    }

    public Boolean getSearchFlag() {
        return searchFlag;
    }

    public void setSearchFlag(Boolean searchFlag) {
        this.searchFlag = searchFlag;
    }

    public String getTombStoneCertificate() {
        return tombStoneCertificate;
    }

    public void setTombStoneCertificate(String tombStoneCertificate) {
        this.tombStoneCertificate = tombStoneCertificate;
    }

    public Integer getMainTabletNum() {
        return mainTabletNum;
    }

    public void setMainTabletNum(Integer mainTabletNum) {
        this.mainTabletNum = mainTabletNum;
    }

    public String getGreenRecord() {
        return greenRecord;
    }

    public void setGreenRecord(String greenRecord) {
        this.greenRecord = greenRecord;
    }

    public String getCleanRecord() {
        return cleanRecord;
    }

    public void setCleanRecord(String cleanRecord) {
        this.cleanRecord = cleanRecord;
    }
}
