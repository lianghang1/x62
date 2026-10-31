package com.scaffold.search.planpck;

import com.scaffold.search.plan.annotation.Column;
import com.scaffold.search.plan.annotation.JoinColumn;
import com.scaffold.search.plan.annotation.JoinTable;
import com.scaffold.search.plan.annotation.QueryPlan;
import com.scaffold.search.plan.annotation.Table;

/**
 * 计划二：returnType 与计划类不同，验证 Map → POJO 自动封装。
 */
@QueryPlan(name = "deptUserSummary", returnType = DeptUserSummary.class)
@Table(name = "t_user", alias = "u")
@JoinTable(name = "t_dept", alias = "d",
        columns = @JoinColumn(name = "id", referencedColumnName = "dept_id"))
public class DeptUserSummaryPlan {

    @Column("u.name")
    private String userName;

    @Column("d.dept_name")
    private String deptName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
}
