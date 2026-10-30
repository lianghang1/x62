package com.scaffold.starter.data.jpa.entity;

import com.scaffold.starter.data.jpa.id.SnowflakeIdentifierGenerator;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.EntityListeners;
import java.time.LocalDateTime;

/**
 * 实体基类，统一提供雪花 Long 主键、审计字段与逻辑删除标记。
 *
 * <p>主键由 {@code SnowflakeIdentifierGenerator} 在应用侧生成，避免数据库自增
 * 带来的分库迁移问题；{@code createBy/updateBy} 由 AuditorAware 自动填充，
 * 来源为 {@code LoginContext} 当前账户，{@code createTime/updateTime} 由
 * Auditing 自动写入，业务代码无需手工赋值。</p>
 *
 * <p>删除一律走 {@code deleted} 标记的逻辑删除，不做物理 delete，查询时
 * 须由 Repository 层按约定过滤。需要乐观锁的实体请继承 {@link VersionedEntity}。</p>
 *
 * @author lianghang
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(generator = "snowflake")
    @GenericGenerator(name = "snowflake", strategy = "com.scaffold.starter.data.jpa.id.SnowflakeIdentifierGenerator")
    private Long id;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @CreatedBy
    @Column(name = "create_by", updatable = false, length = 64)
    private String createBy;

    @LastModifiedBy
    @Column(name = "update_by", length = 64)
    private String updateBy;

    /** 逻辑删除：true=已删除；查询规约由 Repository 层约定兜底。 */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

    /** 标记本实体已逻辑删除（不物理 delete）。 */
    public void markDeleted() {
        this.deleted = Boolean.TRUE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
