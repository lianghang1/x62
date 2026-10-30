package com.scaffold.starter.data.jpa.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

/**
 * 带乐观锁 {@code @Version} 字段的实体基类。
 *
 * <p>存在并发更新同一行的实体（如库存扣减、状态机流转）应继承本类：更新冲突时
 * 抛 {@code ObjectOptimisticLockingFailureException}，由调用方决定重试或报错，
 * 不加锁的场景继续继承 {@code BaseEntity} 即可。</p>
 *
 * @author lianghang
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
