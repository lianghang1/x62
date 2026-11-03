package com.scaffold.server.lite.cemlink.source;

import java.math.BigDecimal;

/**
 * 墓区格位采集查询 Bean，字段名对齐格位采集 SQL 列别名，对应原程序 TombBlockGridQueryBean
 *
 * @author lianghang
 */
public class BlockGridQueryBean {

    private String id;
    private String tombId;
    private String blockId;
    private String layerCode;
    private String displayNo;
    private String tombPath;
    private Integer graveNo;
    private BigDecimal sellArea;
    private BigDecimal greenArea;
    private BigDecimal factArea;
    private BigDecimal buildArea;
    private String tombMaterial;
    private BigDecimal basicPrice;
    private BigDecimal maintenanceFee;
    private BigDecimal innerLength;
    private BigDecimal innerHeight;
    private BigDecimal innerWidth;
    private String isSave;
    private String tombStyle;
    private String buryStyle;

    public String getId() {
        return id;
    }

    public String getTombId() {
        return tombId;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getLayerCode() {
        return layerCode;
    }

    public String getDisplayNo() {
        return displayNo;
    }

    public String getTombPath() {
        return tombPath;
    }

    public Integer getGraveNo() {
        return graveNo;
    }

    public BigDecimal getSellArea() {
        return sellArea;
    }

    public BigDecimal getGreenArea() {
        return greenArea;
    }

    public BigDecimal getFactArea() {
        return factArea;
    }

    public BigDecimal getBuildArea() {
        return buildArea;
    }

    public String getTombMaterial() {
        return tombMaterial;
    }

    public BigDecimal getBasicPrice() {
        return basicPrice;
    }

    public BigDecimal getMaintenanceFee() {
        return maintenanceFee;
    }

    public BigDecimal getInnerLength() {
        return innerLength;
    }

    public BigDecimal getInnerHeight() {
        return innerHeight;
    }

    public BigDecimal getInnerWidth() {
        return innerWidth;
    }

    public String getIsSave() {
        return isSave;
    }

    public String getTombStyle() {
        return tombStyle;
    }

    public String getBuryStyle() {
        return buryStyle;
    }
}
