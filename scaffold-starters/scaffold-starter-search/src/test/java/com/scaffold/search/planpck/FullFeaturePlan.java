package com.scaffold.search.planpck;

import com.scaffold.search.plan.annotation.Column;
import com.scaffold.search.plan.annotation.FieldType;
import com.scaffold.search.plan.annotation.IgnoreProperty;
import com.scaffold.search.plan.annotation.IgnoreProperties;
import com.scaffold.search.plan.annotation.JoinColumn;
import com.scaffold.search.plan.annotation.JoinTable;
import com.scaffold.search.plan.annotation.QueryPlan;
import com.scaffold.search.plan.annotation.Table;

/**
 * 全功能计划：自动映射（无 @Column 字段驼峰转下划线）+ @IgnoreProperty +
 * @IgnoreProperties + @FieldType + useIndex(FORCE INDEX)。
 */
@QueryPlan
@Table(name = "t_user", alias = "u", useIndex = "idx_user_status")
@JoinTable(name = "t_dept", alias = "d", useIndex = "idx_dept_pk",
        columns = @JoinColumn(name = "id", referencedColumnName = "dept_id"))
@IgnoreProperties({
        @IgnoreProperty(name = "internalFlag")
})
@FieldType(name = "memo", type = String.class)
public class FullFeaturePlan {

    // @Column 指定列名
    @Column("u.name")
    private String name;

    // 无注解：自动映射 u.age
    private Integer age;

    // 无注解：自动映射 u.status
    private Integer status;

    // @IgnoreProperties 类级忽略：不参与 SQL
    private Integer internalFlag;

    // @IgnoreProperty 字段级忽略：不参与 SQL
    @IgnoreProperty
    private String secretNote;

    // 类型被类级 @FieldType(name="memo") 覆盖为 String
    private Long memo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getInternalFlag() {
        return internalFlag;
    }

    public void setInternalFlag(Integer internalFlag) {
        this.internalFlag = internalFlag;
    }

    public String getSecretNote() {
        return secretNote;
    }

    public void setSecretNote(String secretNote) {
        this.secretNote = secretNote;
    }

    public Long getMemo() {
        return memo;
    }

    public void setMemo(Long memo) {
        this.memo = memo;
    }
}
