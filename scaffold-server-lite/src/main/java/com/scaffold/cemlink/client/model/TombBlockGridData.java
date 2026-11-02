package com.scaffold.cemlink.client.model;

/**
 * 墓区格位上行 payload，字段对齐 ERP 侧 TombBlockGridData（待 contract jar 扩展后替换）
 *
 * @author lianghang
 */
public class TombBlockGridData {

    private String id;
    private String tombBlockId;
    private String tombId;
    private String layerCode;
    private String displayNo;
    private String tombPath;
    private Integer graveNo;
    private String sellArea;
    private String greenArea;
    private String factArea;
    private String buildArea;
    private String boreSize;
    private String isSave;
    private String tombStyle;
    private String buryStyle;
    private String tombMaterial;
    private Long price;
    private Long maintenancePrice;
    private String status;
    private Integer rowNo;
    private Integer columnNo;
    private String isTomb;
    private String enabled;
    private String picUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTombBlockId() {
        return tombBlockId;
    }

    public void setTombBlockId(String tombBlockId) {
        this.tombBlockId = tombBlockId;
    }

    public String getTombId() {
        return tombId;
    }

    public void setTombId(String tombId) {
        this.tombId = tombId;
    }

    public String getLayerCode() {
        return layerCode;
    }

    public void setLayerCode(String layerCode) {
        this.layerCode = layerCode;
    }

    public String getDisplayNo() {
        return displayNo;
    }

    public void setDisplayNo(String displayNo) {
        this.displayNo = displayNo;
    }

    public String getTombPath() {
        return tombPath;
    }

    public void setTombPath(String tombPath) {
        this.tombPath = tombPath;
    }

    public Integer getGraveNo() {
        return graveNo;
    }

    public void setGraveNo(Integer graveNo) {
        this.graveNo = graveNo;
    }

    public String getSellArea() {
        return sellArea;
    }

    public void setSellArea(String sellArea) {
        this.sellArea = sellArea;
    }

    public String getGreenArea() {
        return greenArea;
    }

    public void setGreenArea(String greenArea) {
        this.greenArea = greenArea;
    }

    public String getFactArea() {
        return factArea;
    }

    public void setFactArea(String factArea) {
        this.factArea = factArea;
    }

    public String getBuildArea() {
        return buildArea;
    }

    public void setBuildArea(String buildArea) {
        this.buildArea = buildArea;
    }

    public String getBoreSize() {
        return boreSize;
    }

    public void setBoreSize(String boreSize) {
        this.boreSize = boreSize;
    }

    public String getIsSave() {
        return isSave;
    }

    public void setIsSave(String isSave) {
        this.isSave = isSave;
    }

    public String getTombStyle() {
        return tombStyle;
    }

    public void setTombStyle(String tombStyle) {
        this.tombStyle = tombStyle;
    }

    public String getBuryStyle() {
        return buryStyle;
    }

    public void setBuryStyle(String buryStyle) {
        this.buryStyle = buryStyle;
    }

    public String getTombMaterial() {
        return tombMaterial;
    }

    public void setTombMaterial(String tombMaterial) {
        this.tombMaterial = tombMaterial;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Long getMaintenancePrice() {
        return maintenancePrice;
    }

    public void setMaintenancePrice(Long maintenancePrice) {
        this.maintenancePrice = maintenancePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getColumnNo() {
        return columnNo;
    }

    public void setColumnNo(Integer columnNo) {
        this.columnNo = columnNo;
    }

    public String getIsTomb() {
        return isTomb;
    }

    public void setIsTomb(String isTomb) {
        this.isTomb = isTomb;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }
}
