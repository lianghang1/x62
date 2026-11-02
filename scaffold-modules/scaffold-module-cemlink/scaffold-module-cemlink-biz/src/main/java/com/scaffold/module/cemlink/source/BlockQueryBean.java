package com.scaffold.module.cemlink.source;

import java.util.Date;

/**
 * 墓区采集查询 Bean，字段名对齐墓区采集 SQL 列别名，对应原程序 TombBlockQueryBean
 *
 * @author lianghang
 */
public class BlockQueryBean {

    private String id;
    private String parentId;
    private String childIds;
    private String blockCode;
    private String blockName;
    private String isBottom;
    private Integer lengthGrid;
    private Integer widthGrid;
    private Date collectionTime;
    private Date modifyDate;

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public String getChildIds() {
        return childIds;
    }

    public String getBlockCode() {
        return blockCode;
    }

    public String getBlockName() {
        return blockName;
    }

    public String getIsBottom() {
        return isBottom;
    }

    public Integer getLengthGrid() {
        return lengthGrid;
    }

    public Integer getWidthGrid() {
        return widthGrid;
    }

    public Date getCollectionTime() {
        return collectionTime;
    }

    public Date getModifyDate() {
        return modifyDate;
    }
}
