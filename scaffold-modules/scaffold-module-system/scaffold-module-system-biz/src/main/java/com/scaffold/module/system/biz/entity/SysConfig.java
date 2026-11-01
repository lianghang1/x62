package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.datachange.DataChange;
import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 系统参数配置实体（sys_config）。
 *
 * <p>运行时可改参数的落库形态：key 全局唯一（小写归一），value 统一字符串
 * （结构化值由读取方解析）；remark 记录用途与取值范围，避免「配置考古」。</p>
 *
 * @author lianghang
 */
@DataChange("系统参数")
@Entity
@Table(name = "sys_config", indexes = {
        @Index(name = "idx_config_key", columnList = "config_key", unique = true)
})
public class SysConfig extends BaseEntity {

    /** 配置键：全局唯一，统一小写存储 */
    @Column(name = "config_key", length = 64, nullable = false)
    private String configKey;

    /** 配置值：统一字符串，读取方按需转型 */
    @Column(name = "config_value", length = 1024)
    private String configValue;

    /** 用途说明与取值范围 */
    @Column(name = "remark", length = 255)
    private String remark;

    /** 是否启用：停用后读取视为不存在 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
