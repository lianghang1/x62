package com.scaffold.module.cemlink.client.model;

import java.util.List;

/**
 * 墓区上行 payload，字段对齐 ERP 侧 TombBlockData（待 contract jar 扩展后替换）
 *
 * @author lianghang
 */
public class TombBlockData {

    private String id;
    private String parentId;
    private String blockCode;
    private String blockName;
    private String isBottom;
    private Integer lengthGrid;
    private Integer widthGrid;
    private Boolean cleanGrid;
    private List<TombBlockGridData> gridDatas;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getBlockCode() {
        return blockCode;
    }

    public void setBlockCode(String blockCode) {
        this.blockCode = blockCode;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public String getIsBottom() {
        return isBottom;
    }

    public void setIsBottom(String isBottom) {
        this.isBottom = isBottom;
    }

    public Integer getLengthGrid() {
        return lengthGrid;
    }

    public void setLengthGrid(Integer lengthGrid) {
        this.lengthGrid = lengthGrid;
    }

    public Integer getWidthGrid() {
        return widthGrid;
    }

    public void setWidthGrid(Integer widthGrid) {
        this.widthGrid = widthGrid;
    }

    public Boolean getCleanGrid() {
        return cleanGrid;
    }

    public void setCleanGrid(Boolean cleanGrid) {
        this.cleanGrid = cleanGrid;
    }

    public List<TombBlockGridData> getGridDatas() {
        return gridDatas;
    }

    public void setGridDatas(List<TombBlockGridData> gridDatas) {
        this.gridDatas = gridDatas;
    }
}
