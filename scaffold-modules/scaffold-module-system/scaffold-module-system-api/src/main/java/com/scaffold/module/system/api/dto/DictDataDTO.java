package com.scaffold.module.system.api.dto;

import java.io.Serializable;

/**
 * 字典项 DTO。
 */
public class DictDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 字典类型，如 order_status */
    private String type;

    /** 显示文本 */
    private String label;

    /** 存储值 */
    private String value;

    private Integer sort;

    private Boolean enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
