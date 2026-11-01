package com.scaffold.starter.data.jpa.datachange;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 留痕测试实体：未标注 {@link DataChange}，验证不产生任何事件。
 *
 * @author lianghang
 */
@Entity
@Table(name = "untracked_record")
public class UntrackedRecord extends BaseEntity {

    @Column(name = "name", length = 64)
    private String name;

    protected UntrackedRecord() {
    }

    public UntrackedRecord(String name) {
        this.name = name;
    }
}
