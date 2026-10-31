package com.scaffold.search;

import com.scaffold.search.filter.Operator;
import com.scaffold.search.filter.SearchFilter;
import com.scaffold.search.plan.QueryPlanRegistry;
import com.scaffold.search.planpck.DeptUserSummary;
import com.scaffold.search.planpck.DeptUserSummaryPlan;
import com.scaffold.search.planpck.OrderView;
import com.scaffold.search.planpck.UserPlan;
import com.scaffold.search.planpck.UserVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = TestApp.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:plandb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "scaffold.search.plan-packages=com.scaffold.search.planpck"
})
class QueryPlanTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void createTables(@Autowired JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_dept (" +
                "id BIGINT PRIMARY KEY, dept_name VARCHAR(100))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_user (" +
                "id BIGINT PRIMARY KEY, name VARCHAR(100), age INT, status INT, dept_id BIGINT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_order (" +
                "id BIGINT PRIMARY KEY, order_no VARCHAR(50), amount INT, user_id BIGINT, deleted INT)");
    }

    @BeforeEach
    void cleanAndInsert() {
        jdbcTemplate.update("DELETE FROM t_user");
        jdbcTemplate.update("DELETE FROM t_dept");
        jdbcTemplate.update("DELETE FROM t_order");
        jdbcTemplate.update("INSERT INTO t_dept VALUES (1, '研发部'), (2, '市场部')");
        jdbcTemplate.update("INSERT INTO t_user VALUES " +
                "(1, '张三', 25, 1, 1)," +
                "(2, '张四', 30, 1, 1)," +
                "(3, '李五', 35, 0, 2)," +
                "(4, '王六', 28, 1, 2)");
        jdbcTemplate.update("INSERT INTO t_order VALUES " +
                "(1, 'SO-001', 100, 1, 0)," +
                "(2, 'SO-002', 200, 2, 0)," +
                "(3, 'SO-003', 300, 3, 1)");  // 已删除，固定条件应过滤
    }

    @Test
    void testPlanRegisteredByName() {
        assertSame(UserPlan.class, QueryPlanRegistry.get("userPlan"));
        assertSame(DeptUserSummaryPlan.class, QueryPlanRegistry.require("deptUserSummary"));
        assertThrows(IllegalArgumentException.class, () -> QueryPlanRegistry.require("notExist"));
    }

    @Test
    void testPlanJoinAndFixedFilter() {
        // 固定条件 status=1 生效：李五(status=0)不可见
        List<UserPlan> list = QueryUtils.doListQuery("userPlan", null, null);
        assertEquals(3, list.size());
        // 联表字段
        UserPlan first = list.get(0);
        assertNotNull(first.getDeptName());
        // 动态条件走 @Column 属性名
        long count = QueryUtils.doCount("userPlan",
                java.util.Collections.singletonList(
                        new SearchFilter("name", Operator.EQ, "张三")));
        assertEquals(1, count);
    }

    @Test
    void testPlanPaged() {
        Page<UserPlan> page = QueryUtils.doQuery("userPlan", null, PageRequest.of(0, 2));
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void testPlanWithReturnType() {
        List<DeptUserSummary> list = QueryUtils.doListQuery("deptUserSummary", null, null);
        assertEquals(4, list.size());
        DeptUserSummary summary = list.get(0);
        assertNotNull(summary.getUserName());
        assertNotNull(summary.getDeptName());
    }

    @Test
    void testDirectPlanClass() {
        List<UserPlan> list = QueryUtils.doListQuery(UserPlan.class,
                java.util.Collections.singletonList(
                        new SearchFilter(UserPlan::getAge, Operator.GE, 28)), null);
        assertEquals(2, list.size());
    }

    @Test
    void testMultiJoinTablePlan() {
        // 双 @JoinTable（o→u inner join，o→d 自定义 ON）+ 固定条件 deleted=0 + returnType 封装
        List<OrderView> list = QueryUtils.doListQuery("orderPlan", null, null);
        assertEquals(2, list.size());
        assertEquals("SO-001", list.get(0).getOrderNo());
        assertEquals("张三", list.get(0).getUserName());
        assertEquals("研发部", list.get(0).getDeptName());
        // 动态条件 + 排序
        List<OrderView> filtered = QueryUtils.doListQuery("orderPlan",
                java.util.Collections.singletonList(
                        new SearchFilter("amount", Operator.GE, 200)), null);
        assertEquals(1, filtered.size());
        assertEquals("张四", filtered.get(0).getUserName());
    }
}
