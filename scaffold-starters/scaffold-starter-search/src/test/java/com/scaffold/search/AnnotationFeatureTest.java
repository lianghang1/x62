package com.scaffold.search;

import com.scaffold.search.filter.Operator;
import com.scaffold.search.filter.SearchFilter;
import com.scaffold.search.planpck.AttrDemoPlan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 注解属性全覆盖验证：@Column（value/name/table/SQL 表达式）、
 * @JoinColumn（referencedColumnName/sql/referencedValue 混合）、
 * @JoinTable joinType、@SearchFilter type、@FieldType、@IgnoreProperty。
 */
@SpringBootTest(classes = TestApp.class)
class AnnotationFeatureTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void createTables(@Autowired JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_order (" +
                "order_id BIGINT PRIMARY KEY, order_no VARCHAR(50), buyer_no VARCHAR(50), " +
                "status INT, price INT, quantity INT, remark VARCHAR(100))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_member (" +
                "member_id BIGINT PRIMARY KEY, member_no VARCHAR(50), name VARCHAR(100), level INT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_label (" +
                "label_key INT, label_type VARCHAR(50), label_value VARCHAR(100))");
    }

    @BeforeEach
    void cleanAndInsert() {
        jdbcTemplate.update("DELETE FROM t_order");
        jdbcTemplate.update("DELETE FROM t_member");
        jdbcTemplate.update("DELETE FROM t_label");
        // 固定条件 status >= 2 会过滤掉 status=1 的行
        jdbcTemplate.update("INSERT INTO t_order VALUES " +
                "(1, 'A001', 'M01', 2, 100, 3, 'r1')," +   // 总额 300
                "(2, 'A002', 'M02', 5, 200, 2, 'r2')," +   // 总额 400
                "(3, 'A003', 'M01', 1, 50, 1, 'r3')");     // 被固定条件过滤
        jdbcTemplate.update("INSERT INTO t_member VALUES " +
                "(11, 'M01', '张三', 1)," +
                "(12, 'M02', '李四', 5)");
        jdbcTemplate.update("INSERT INTO t_label VALUES " +
                "(2, 'order', '待发货')," +
                "(5, 'order', '已完成')," +
                "(9, 'user', '无关标签')");
    }

    /**
     * 一次性验证：SQL 表达式列、table= 前缀列、自动映射、inner join（非主键关联）、
     * 混合 ON（sql + referencedValue 常量）、固定条件 type 转换、字典翻译。
     */
    @Test
    void testAllAttributes() {
        List<AttrDemoPlan> list = QueryUtils.doListQuery(AttrDemoPlan.class,
                Collections.singletonList(
                        new SearchFilter("quantity", Operator.GE, 2)), null);

        // 固定条件 status >= 2（type=Integer 解析 "2"）+ quantity >= 2 → 订单 1、2
        list.forEach(r -> System.out.println("ROW>> " + r.getOrderNo() + " st=" + r.getStatus() + " qty=" + r.getQuantity()));
        assertEquals(2, list.size());

        AttrDemoPlan first = list.get(0);
        // SQL 表达式列：o.price * o.quantity
        assertEquals(300, first.getTotalAmount());
        // @Column(name, table="m")
        assertEquals("张三", first.getMemberName());
        // 自动映射字段回填
        assertEquals(3, first.getQuantity());
        // @FieldType 类型覆盖后正常返回
        assertEquals(1, first.getLevel());
        // inner join + 非主键关联（m.member_no = o.buyer_no）
        assertEquals("A001", first.getOrderNo());
        // 字典翻译：sql ON（l.label_key = o.status）+ referencedValue 常量（l.label_type = 'order'）
        assertEquals("待发货", first.getStatusLabel());

        AttrDemoPlan second = list.get(1);
        assertEquals(400, second.getTotalAmount());
        assertEquals("李四", second.getMemberName());
        assertEquals("已完成", second.getStatusLabel());
    }

    /** 固定条件总是生效：同字段的动态参数被丢弃（FixedFilterParamFilter），而非叠加 */
    @Test
    void testFixedFilterCannotBeOverridden() {
        List<SearchFilter> params = Collections.singletonList(
                new SearchFilter("status", Operator.EQ, 1));
        // 前端传 status=1 被丢弃，只剩固定条件 status >= 2 → 仍 2 行
        assertEquals(2, QueryUtils.doCount(params, AttrDemoPlan.class));
        // 无显式条件时固定条件同样生效：status >= 2 → 2 行
        assertEquals(2, QueryUtils.doCount(Collections.emptyList(), AttrDemoPlan.class));
    }
}
