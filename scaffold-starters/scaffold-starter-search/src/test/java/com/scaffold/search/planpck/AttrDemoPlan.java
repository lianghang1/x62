package com.scaffold.search.planpck;

import com.scaffold.search.plan.annotation.Column;
import com.scaffold.search.plan.annotation.FieldType;
import com.scaffold.search.plan.annotation.IgnoreProperties;
import com.scaffold.search.plan.annotation.IgnoreProperty;
import com.scaffold.search.plan.annotation.JoinColumn;
import com.scaffold.search.plan.annotation.JoinTable;
import com.scaffold.search.plan.annotation.QueryPlan;
import com.scaffold.search.plan.annotation.SearchFilter;
import com.scaffold.search.plan.annotation.Table;

/**
 * 注解属性全覆盖演示计划：每种注解属性都有对应字段。
 *
 * <p>表结构（AnnotationFeatureTest 建表）：
 * <pre>
 * t_order(order_id, order_no, buyer_no, status, price, quantity, remark)
 * t_member(member_id, member_no, name, level)
 * t_label(label_key, label_type, label_value)
 * </pre>
 */
@QueryPlan(name = "attrDemoPlan")
@Table(name = "t_order", alias = "o",
        filters = @SearchFilter(name = "status", op = com.scaffold.search.filter.Operator.GE,
                value = "2", type = Integer.class))          // 固定条件：type 指定按 Integer 解析 "2"
@JoinTable(name = "t_member", alias = "m", joinType = "inner join",
        columns = @JoinColumn(name = "member_no", referencedColumnName = "buyer_no"))  // m.member_no = o.buyer_no（非主键关联）
@JoinTable(name = "t_label", alias = "l", joinType = "left join",
        columns = {
                // 混合三种 ON 模式：字段对字段 + 常量
                @JoinColumn(sql = "l.label_key = o.status"),
                @JoinColumn(name = "label_type", referencedValue = "order")  // l.label_type = 'order'
        })
@FieldType(name = "level", type = String.class)          // 类级类型覆盖：level 按 String 处理
@IgnoreProperties({ @IgnoreProperty(name = "remark") })  // 类级忽略
public class AttrDemoPlan {

    // ---- @Column 的三种写法 ----

    /** 写法 1：value 简写 */
    @Column("o.order_no")
    private String orderNo;

    /** 写法 2：name 属性（等价 value） */
    @Column(name = "o.price * o.quantity")               // SQL 表达式：任意 SQL 片段
    private Integer totalAmount;

    /** 写法 3：name + table（table 指定别名前缀） */
    @Column(name = "name", table = "m")                  // → m.name
    private String memberName;

    // ---- 自动映射（未标注字段：驼峰转下划线 + 主表别名）----

    /** 自动映射 o.status（固定条件引用的字段必须有映射，否则条件无处挂载） */
    private Integer status;

    private Integer quantity;                            // → o.quantity

    /** 字段级忽略（与类级等价，二选一即可） */
    @IgnoreProperty
    private String internalRemark;                       // 不进 SQL

    /** 写法 3：name + table（table 指定别名前缀，关联表字段必须显式标注——自动映射只到主表） */
    @Column(name = "level", table = "m")                 // → m.level
    private Integer level;

    // ---- 关联表字段 ----

    @Column("l.label_value")
    private String statusLabel;                          // 状态字典翻译

    public String getOrderNo() {
        return orderNo;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public String getMemberName() {
        return memberName;
    }

    public Integer getStatus() {
        return status;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getLevel() {
        return level;
    }

    public String getStatusLabel() {
        return statusLabel;
    }
}
