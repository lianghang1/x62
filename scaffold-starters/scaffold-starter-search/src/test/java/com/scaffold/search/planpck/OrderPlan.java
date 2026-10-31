package com.scaffold.search.planpck;

import com.scaffold.search.filter.Operator;
import com.scaffold.search.plan.annotation.Column;
import com.scaffold.search.plan.annotation.JoinColumn;
import com.scaffold.search.plan.annotation.JoinTable;
import com.scaffold.search.plan.annotation.QueryPlan;
import com.scaffold.search.plan.annotation.SearchFilter;
import com.scaffold.search.plan.annotation.Table;

/**
 * 多表联查计划：订单 → 用户（inner join）→ 部门（自定义 ON 条件），
 * 验证 @JoinTable 可重复标注 + 固定条件 + inner join。
 */
@QueryPlan(name = "orderPlan", returnType = OrderView.class)
@Table(name = "t_order", alias = "o",
        filters = @SearchFilter(name = "deleted", op = Operator.EQ, value = "0", type = Integer.class))
@JoinTable(name = "t_user", alias = "u", joinType = "inner join",
        columns = @JoinColumn(name = "id", referencedColumnName = "user_id"))
@JoinTable(name = "t_dept", alias = "d",
        columns = @JoinColumn(sql = "d.id = u.dept_id"))
public class OrderPlan {

    @Column("o.order_no")
    private String orderNo;

    @Column("o.amount")
    private Integer amount;

    @Column("o.deleted")
    private Integer deleted;

    @Column("u.name")
    private String userName;

    @Column("d.dept_name")
    private String deptName;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Integer getAmount() {
        return amount;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

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
