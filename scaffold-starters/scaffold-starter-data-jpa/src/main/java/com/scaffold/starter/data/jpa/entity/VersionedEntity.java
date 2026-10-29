package com.scaffold.starter.data.jpa.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

/**
 * 带乐观锁的实体基类：存在并发更新同一行（如库存扣减、状态机流转）的实体继承本类，
 * 更新冲突时抛 ObjectOptimisticLockingFailureException，由调用方决定重试或报错。
 */
@MappedSuperclass
public abstract class VersionedEntity extends BaseEntity {

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
