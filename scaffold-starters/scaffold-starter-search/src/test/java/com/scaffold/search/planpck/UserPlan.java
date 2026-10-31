package com.scaffold.search.planpck;

import com.scaffold.search.plan.annotation.Column;
import com.scaffold.search.plan.annotation.JoinColumn;
import com.scaffold.search.plan.annotation.JoinTable;
import com.scaffold.search.plan.annotation.QueryPlan;
import com.scaffold.search.plan.annotation.Table;

/**
 * 计划一：用户联表计划（直接返回计划类实例），带固定条件 status=1。
 */
@QueryPlan(name = "userPlan")
@Table(name = "t_user", alias = "u",
        filters = @Table.Fixed(name = "status", op = com.scaffold.search.filter.Operator.EQ,
                value = "1", type = Integer.class))
@JoinTable(name = "t_dept", alias = "d",
        columns = @JoinColumn(name = "id", referencedColumnName = "dept_id"))
public class UserPlan {

    @Column("u.id")
    private Long id;

    @Column("u.name")
    private String name;

    @Column("u.age")
    private Integer age;

    @Column("u.status")
    private Integer status;

    @Column("d.dept_name")
    private String deptName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
}
