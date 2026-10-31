package com.scaffold.starter.data.jpa.datascope;

import com.scaffold.starter.data.jpa.entity.BaseEntity;
import org.hibernate.annotations.Filter;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 数据权限测试实体：@Filter 白名单加入 SELF 过滤。
 *
 * @author lianghang
 */
@Entity
@Table(name = "TBL_SCOPED")
@Filter(name = DataScopeAspect.FILTER_SELF)
public class ScopedRecord extends BaseEntity {

    private String name;

    protected ScopedRecord() {
    }

    public ScopedRecord(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
