package com.scaffold.search;

import com.scaffold.search.filter.Operator;
import com.scaffold.search.filter.SearchFilter;
import com.scaffold.search.planpck.FullFeaturePlan;
import com.scaffold.search.planpck.UserVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestApp.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:fulldb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "scaffold.search.plan-packages=com.scaffold.search.planpck"
})
class FullFeatureTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void createTables(@Autowired JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_dept (" +
                "id BIGINT PRIMARY KEY, dept_name VARCHAR(100))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_user (" +
                "id BIGINT PRIMARY KEY, name VARCHAR(100), age INT, status INT, dept_id BIGINT, " +
                "internal_flag INT, secret_note VARCHAR(100), memo BIGINT, deleted_at TIMESTAMP NULL)");
    }

    @BeforeEach
    void cleanAndInsert() {
        jdbcTemplate.update("DELETE FROM t_user");
        jdbcTemplate.update("DELETE FROM t_dept");
        jdbcTemplate.update("INSERT INTO t_dept VALUES (1, '研发部'), (2, '市场部')");
        jdbcTemplate.update("INSERT INTO t_user VALUES " +
                "(1, '张三', 25, 1, 1, 0, 'a', 111, NULL)," +
                "(2, '张四', 30, 1, 1, 1, 'b', 222, NULL)," +
                "(3, '李五', 35, 0, 2, 0, 'c', 333, NULL)," +
                "(4, '王六', 28, 1, 2, 0, 'd', 444, NULL)");
    }

    /**
     * 全功能计划合成校验（单元级，不依赖数据库方言支持 FORCE INDEX）：
     * 自动映射 + 忽略 + @FieldType + FORCE INDEX 拼接。
     */
    @Test
    void testFullFeaturePlanMeta() {
        cn.zhxu.bs.BeanMeta<?> meta = new com.scaffold.search.plan.ScaffoldMetaResolver(
                new cn.zhxu.bs.implement.DefaultMetaResolver()).resolve(FullFeaturePlan.class);

        String tables = meta.getTableSnippet().getSql();
        assertTrue(tables.contains("t_user u FORCE INDEX(idx_user_status)"));
        assertTrue(tables.contains("left join t_dept d FORCE INDEX(idx_dept_pk) on (d.id = u.dept_id)"));

        Map<String, String> fieldSqls = new HashMap<>();
        for (cn.zhxu.bs.FieldMeta fm : meta.getFieldMetas()) {
            fieldSqls.put(fm.getName(), fm.getFieldSql() == null ? "" : fm.getFieldSql().getSql());
        }
        // @Column 指定列
        assertEquals("u.name", fieldSqls.get("name"));
        // 无注解自动映射：驼峰转下划线 + 主表别名
        assertEquals("u.age", fieldSqls.get("age"));
        assertEquals("u.status", fieldSqls.get("status"));
        // @IgnoreProperty / @IgnoreProperties：不参与 SQL
        assertTrue(!fieldSqls.containsKey("internalFlag"));
        assertTrue(!fieldSqls.containsKey("secretNote"));
        // @FieldType 覆盖的类型字段仍在
        assertTrue(fieldSqls.containsKey("memo"));
    }

    @Test
    void testParsePrefixProtocol() {
        Map<String, String> params = new HashMap<>();
        params.put("EQ_status", "1");
        params.put("CN_name", "张");
        params.put("GE_age", "");
        params.put("XX_age", "99");          // 非法运算符：跳过
        params.put("keyword", "hello");      // 非协议 key：跳过
        List<SearchFilter> filters = QueryUtils.parse(params);
        assertEquals(2, filters.size());
        assertEquals(2, QueryUtils.doCount(filters, UserVO.class));

        // OR 分支协议：status=0 OR age>=30
        Map<String, String> orParams = new HashMap<>();
        orParams.put("EQ_status", "0||GE_age||30");
        List<SearchFilter> orFilters = QueryUtils.parse(orParams);
        assertEquals(1, orFilters.size());
        assertTrue(orFilters.get(0).isOrGroup());
        assertEquals(2, QueryUtils.doCount(orFilters, UserVO.class)); // 李五(0) + 张四/李五(>=30)

        // 无值运算符（同字段多条件在参数协议中会互相覆盖，需分开断言）
        Map<String, String> nnParams = new HashMap<>();
        nnParams.put("NN_id", "");
        assertEquals(4, QueryUtils.doCount(QueryUtils.parse(nnParams), UserVO.class));
        Map<String, String> nuParams = new HashMap<>();
        nuParams.put("NU_id", "");
        assertEquals(0, QueryUtils.doCount(QueryUtils.parse(nuParams), UserVO.class));
    }

    @Test
    void testOrFiltersOverload() {
        List<SearchFilter> ors = Arrays.asList(
                new SearchFilter("name", Operator.EQ, "张三"),
                new SearchFilter("name", Operator.EQ, "王六"));
        long count = QueryUtils.doCount(
                java.util.Collections.singletonList(new SearchFilter("status", Operator.EQ, 1)),
                ors, UserVO.class);
        assertEquals(2, count);

        List<UserVO> list = QueryUtils.doListQuery(
                java.util.Collections.singletonList(new SearchFilter("status", Operator.EQ, 1)),
                ors, UserVO.class);
        assertEquals(2, list.size());
    }

    @Test
    void testSortOnlyOverload() {
        List<UserVO> list = QueryUtils.doListQuery(
                java.util.Collections.singletonList(new SearchFilter("status", Operator.EQ, 1)),
                Sort.by(Sort.Direction.DESC, "age"), UserVO.class);
        assertEquals(3, list.size());
        assertEquals(30, list.get(0).getAge());
    }

    @Test
    void testNativeQuery() {
        Map<String, Object> params = new HashMap<>();
        params.put("minAge", 26);
        List<UserVO> list = QueryUtils.doNativeQuery(
                "select u.id, u.name, u.age, u.status, d.dept_name " +
                        "from t_user u left join t_dept d on u.dept_id = d.id where u.age >= :minAge " +
                        "order by u.age desc",
                params, UserVO.class);
        assertEquals(3, list.size());
        assertEquals("李五", list.get(0).getName());       // 35 最大
        assertEquals("市场部", list.get(0).getDeptName());  // 下划线列自动映射驼峰属性

        // 分页（LIMIT/OFFSET）
        Page<UserVO> page = new PageImpl<>(QueryUtils.doNativeQuery(
                "select u.id, u.name, u.age, u.status from t_user u order by u.id",
                null, PageRequest.of(1, 2), UserVO.class),
                PageRequest.of(1, 2), 4);
        assertEquals(2, page.getContent().size());
        assertEquals(3L, page.getContent().get(0).getId().longValue());

        // 原生计数
        long count = QueryUtils.doNativeCount(
                "select u.id from t_user u where u.age >= 26", null);
        assertEquals(3, count);
    }
}
