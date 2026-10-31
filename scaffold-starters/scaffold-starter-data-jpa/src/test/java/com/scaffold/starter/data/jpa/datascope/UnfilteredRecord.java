package com.scaffold.starter.data.jpa.datascope;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 数据权限测试实体：白名单外，不标 @Filter，任何 Scope 下不受影响。
 *
 * @author lianghang
 */
@Entity
@Table(name = "TBL_UNFILTERED")
public class UnfilteredRecord extends BaseEntity {

    private String name;

    protected UnfilteredRecord() {
    }

    public UnfilteredRecord(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
