package com.scaffold.starter.data.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

/**
 * 动态数据源测试实体：独立于 BaseEntity，主键走 H2 自增，聚焦路由行为本身。
 *
 * @author lianghang
 */
@Entity
@Table(name = "DYN_RECORD")
public class DynRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected DynRecord() {
    }

    public DynRecord(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DynRecord)) {
            return false;
        }
        DynRecord that = (DynRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
