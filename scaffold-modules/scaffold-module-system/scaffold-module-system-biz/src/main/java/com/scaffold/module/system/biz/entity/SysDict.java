package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 字典项（业务性横切的提供方实体，跨模块经 DictApi 使用）。
 */
@Entity
@Table(name = "sys_dict")
public class SysDict extends BaseEntity {

    @Column(name = "dict_type", length = 64, nullable = false)
    private String type;

    @Column(name = "label", length = 64, nullable = false)
    private String label;

    @Column(name = "value", length = 64, nullable = false)
    private String value;

    @Column(name = "sort_no", nullable = false)
    private Integer sort = 0;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

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
