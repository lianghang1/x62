package com.scaffold.server.lite.cemlink.source;

import java.math.BigDecimal;

/**
 * 墓穴采集查询 Bean，字段名对齐采集 SQL 列别名，对应原程序 TombQueryBean
 *
 * @author lianghang
 */
public class TombQueryBean {

    private String id;
    private String layerCode;
    private String tombPath;
    private String blockId;
    private Integer graveNo;
    private String tombStyle;
    private String buryStyle;
    private Boolean outdoorFlag;
    private BigDecimal factArea;
    private BigDecimal sellArea;
    private BigDecimal basicPrice;
    private String appDate;
    private String payStatus;
    private String name;
    private String mobileNo;
    private String tombId;
    private Integer maintenanceYear;
    private BigDecimal maintenanceFee;
    private Integer collectVersion;

    public String getId() {
        return id;
    }

    public String getLayerCode() {
        return layerCode;
    }

    public String getTombPath() {
        return tombPath;
    }

    public String getBlockId() {
        return blockId;
    }

    public Integer getGraveNo() {
        return graveNo;
    }

    public String getTombStyle() {
        return tombStyle;
    }

    public String getBuryStyle() {
        return buryStyle;
    }

    public Boolean getOutdoorFlag() {
        return outdoorFlag;
    }

    public BigDecimal getFactArea() {
        return factArea;
    }

    public BigDecimal getSellArea() {
        return sellArea;
    }

    public BigDecimal getBasicPrice() {
        return basicPrice;
    }

    public String getAppDate() {
        return appDate;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public String getName() {
        return name;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public String getTombId() {
        return tombId;
    }

    public Integer getMaintenanceYear() {
        return maintenanceYear;
    }

    public BigDecimal getMaintenanceFee() {
        return maintenanceFee;
    }

    public Integer getCollectVersion() {
        return collectVersion;
    }
}
