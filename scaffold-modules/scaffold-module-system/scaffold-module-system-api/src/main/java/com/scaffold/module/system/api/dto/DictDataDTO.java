package com.scaffold.module.system.api.dto;

import java.io.Serializable;

/**
 * 字典项 DTO。
 *
 * <p>承载单条字典数据（类型 / 标签 / 值 / 排序 / 启用状态），
 * 供 {@code DictApi} 对外输出，屏蔽字典表结构细节。</p>
 *
 * @author lianghang
 */
public class DictDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 字典类型，如 {@code order_status}。 */
    private String type;

    /** 显示文本。 */
    private String label;

    /** 存储值。 */
    private String value;

    /** 排序号，越小越靠前。 */
    private Integer sort;

    /** 是否启用。 */
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
