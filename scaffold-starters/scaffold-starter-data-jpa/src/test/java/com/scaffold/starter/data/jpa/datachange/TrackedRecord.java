package com.scaffold.starter.data.jpa.datachange;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 留痕测试实体：标注 {@link DataChange}，验证增/改/删三类事件。
 *
 * @author lianghang
 */
@Entity
@Table(name = "tracked_record")
@DataChange("测试对象")
public class TrackedRecord extends BaseEntity {

    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "age")
    private Integer age;

    protected TrackedRecord() {
    }

    public TrackedRecord(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
