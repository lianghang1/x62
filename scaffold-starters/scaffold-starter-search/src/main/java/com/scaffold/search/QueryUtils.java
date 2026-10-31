package com.scaffold.search;

import cn.zhxu.bs.BeanSearcher;
import cn.zhxu.bs.MapSearcher;
import cn.zhxu.bs.SearchResult;
import cn.zhxu.bs.util.MapBuilder;
import cn.zhxu.bs.util.MapUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.search.filter.Operator;
import com.scaffold.search.filter.SearchFilter;
import com.scaffold.search.plan.QueryPlanRegistry;
import com.scaffold.search.plan.annotation.QueryPlan;
import com.scaffold.search.plan.annotation.Table;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 动态检索统一入口（静态门面，用法对齐 db-spring QueryUtils，底层 Bean Searcher）。
 *
 * <p>由 {@code SearchAutoConfiguration} 在启动时注入检索器；同事务：
 * Bean Searcher 默认使用 SpringSqlExecutor，在 {@code @Transactional} 方法内
 * 自动复用事务连接。
 *
 * <pre>{@code
 * List<SearchFilter> filters = new ArrayList<>();
 * filters.add(new SearchFilter("status", Operator.EQ, 1));
 *
 * Page<UserVO> page = QueryUtils.doQuery(filters, PageRequest.of(0, 15), UserVO.class);
 * List<UserVO> list = QueryUtils.doListQuery(filters, UserVO.class);
 * long count       = QueryUtils.doCount(filters, UserVO.class);
 *
 * // QueryPlan（命名联表查询计划）
 * Page<DeptUserVO> p = QueryUtils.doQuery("userPlan", filters, PageRequest.of(0, 15));
 * }</pre>
 */
public final class QueryUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static volatile BeanSearcher beanSearcher;
    private static volatile MapSearcher mapSearcher;
    private static volatile NamedParameterJdbcTemplate namedJdbc;

    private QueryUtils() {
    }

    /** 由自动配置调用，应用内不要使用 */
    public static void configure(BeanSearcher beanSearcher, MapSearcher mapSearcher) {
        QueryUtils.beanSearcher = beanSearcher;
        QueryUtils.mapSearcher = mapSearcher;
    }

    /** 由自动配置调用（spring-jdbc 环境存在时），应用内不要使用 */
    public static void configureNative(NamedParameterJdbcTemplate namedJdbc) {
        QueryUtils.namedJdbc = namedJdbc;
    }

    private static BeanSearcher searcher() {
        BeanSearcher s = beanSearcher;
        if (s == null) {
            throw new IllegalStateException("QueryUtils 未初始化：请引入 scaffold-starter-search 并保持 scaffold.search.enabled=true");
        }
        return s;
    }

    private static MapSearcher mapSearcher() {
        MapSearcher s = mapSearcher;
        if (s == null) {
            throw new IllegalStateException("QueryUtils 未初始化：请引入 scaffold-starter-search 并保持 scaffold.search.enabled=true");
        }
        return s;
    }

    // ==================== 分页查询（返回 Page） ====================

    public static <T> Page<T> doQuery(List<SearchFilter> filters, Pageable pageable, Class<T> beanClass) {
        return doQuery(beanClass, filters, pageable);
    }

    public static <T> Page<T> doQuery(Class<T> beanClass, List<SearchFilter> filters, Pageable pageable) {
        MapBuilder mb = builder(beanClass, filters, pageable);
        if (isPlanWithReturnType(beanClass)) {
            if (isPaged(pageable)) {
                SearchResult<Map<String, Object>> result = mapSearcher().search(beanClass, mb.build());
                List<T> list = convertPlanRows(beanClass, result.getDataList());
                return new PageImpl<>(list, pageable, result.getTotalCount().longValue());
            }
            List<T> list = convertPlanRows(beanClass, mapSearcher().searchAll(beanClass, mb.build()));
            return new PageImpl<>(list, Pageable.unpaged(), list.size());
        }
        if (isPaged(pageable)) {
            SearchResult<T> result = searcher().search(beanClass, mb.build());
            return new PageImpl<>(result.getDataList(), pageable, result.getTotalCount().longValue());
        }
        // 未分页：全量 + 计数
        long total = doCount(beanClass, filters);
        List<T> list = searcher().searchAll(beanClass, mb.build());
        return new PageImpl<>(list, Pageable.unpaged(), total);
    }

    /**
     * 按计划名分页查询（QueryPlan 注册表查找）。
     */
    @SuppressWarnings("unchecked")
    public static <T> Page<T> doQuery(String planName, List<SearchFilter> filters, Pageable pageable) {
        return (Page<T>) doQuery(QueryPlanRegistry.require(planName), filters, pageable);
    }

    // ==================== 列表查询 ====================

    public static <T> List<T> doListQuery(List<SearchFilter> filters, Class<T> beanClass) {
        return doListQuery(beanClass, filters, null);
    }

    public static <T> List<T> doListQuery(List<SearchFilter> filters, Pageable pageable, Class<T> beanClass) {
        return doListQuery(beanClass, filters, pageable);
    }

    public static <T> List<T> doListQuery(Class<T> beanClass, List<SearchFilter> filters, Pageable pageable) {
        MapBuilder mb = builder(beanClass, filters, pageable);
        if (isPlanWithReturnType(beanClass)) {
            Map<String, Object> params = mb.build();
            if (isPaged(pageable)) {
                return convertPlanRows(beanClass, mapSearcher().searchList(beanClass, params));
            }
            return convertPlanRows(beanClass, mapSearcher().searchAll(beanClass, params));
        }
        if (isPaged(pageable)) {
            return searcher().searchList(beanClass, mb.build());
        }
        return searcher().searchAll(beanClass, mb.build());
    }

    /**
     * 按计划名列表查询（QueryPlan 注册表查找）。
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> doListQuery(String planName, List<SearchFilter> filters, Pageable pageable) {
        return (List<T>) doListQuery(QueryPlanRegistry.require(planName), filters, pageable);
    }

    // ==================== 计数 ====================

    public static long doCount(List<SearchFilter> filters, Class<?> beanClass) {
        return doCount(beanClass, filters);
    }

    public static long doCount(Class<?> beanClass, List<SearchFilter> filters) {
        MapBuilder mb = builder(beanClass, filters, null);
        SearchResult<?> result = searcher().search(beanClass, mb.build());
        return result.getTotalCount().longValue();
    }

    public static long doCount(String planName, List<SearchFilter> filters) {
        return doCount(QueryPlanRegistry.require(planName), filters);
    }

    // ==================== orFilters 重载（对齐 db-spring buildSpecification(filters, orFilters)） ====================

    public static <T> Page<T> doQuery(List<SearchFilter> filters, List<SearchFilter> orFilters,
                                      Pageable pageable, Class<T> beanClass) {
        MapBuilder mb = builder(beanClass, filters, orFilters, pageable, pageable == null ? null : pageable.getSort());
        if (isPaged(pageable)) {
            SearchResult<T> result = searcher().search(beanClass, mb.build());
            return new PageImpl<>(result.getDataList(), pageable, result.getTotalCount().longValue());
        }
        long total = doCount(filters, orFilters, beanClass);
        List<T> list = searcher().searchAll(beanClass, mb.build());
        return new PageImpl<>(list, Pageable.unpaged(), total);
    }

    public static <T> List<T> doListQuery(List<SearchFilter> filters, List<SearchFilter> orFilters, Class<T> beanClass) {
        return searcher().searchAll(beanClass, builder(beanClass, filters, orFilters, null, null).build());
    }

    public static long doCount(List<SearchFilter> filters, List<SearchFilter> orFilters, Class<?> beanClass) {
        SearchResult<?> result = searcher().search(beanClass,
                builder(beanClass, filters, orFilters, null, null).build());
        return result.getTotalCount().longValue();
    }

    // ==================== 仅排序重载 ====================

    /**
     * 只排序不分页的列表查询。
     */
    public static <T> List<T> doListQuery(List<SearchFilter> filters, Sort sort, Class<T> beanClass) {
        return searcher().searchAll(beanClass, builder(beanClass, filters, null, null, sort).build());
    }

    // ==================== 前端参数协议（对齐 db-spring SearchUtils.parse） ====================

    /**
     * 解析前端参数为过滤器列表（db-spring 前缀协议）：
     * key = {@code 运算符_字段名}（如 {@code EQ_status}、{@code CN_name}），
     * value 支持 OR 分支：{@code "v1||GT_age||30"} → {@code v1 OR age>30}；
     * 无值运算符（NU/NN/BLANK/NBLANK）value 传空串；空值条件整体跳过。
     *
     * <pre>{@code
     * // GET /user?EQ_status=1&CN_name=张
     * List<SearchFilter> filters = QueryUtils.parse(request.getParameterMap());
     * QueryUtils.doQuery(filters, pageable, UserVO.class);
     * }</pre>
     */
    public static List<SearchFilter> parse(Map<String, ?> searchParams) {
        List<SearchFilter> filters = new ArrayList<>();
        if (searchParams == null) {
            return filters;
        }
        for (Map.Entry<String, ?> entry : searchParams.entrySet()) {
            String[] parts = entry.getKey().split("_", 2);
            if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                continue;
            }
            Operator op;
            try {
                op = Operator.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                continue; // 非法运算符：跳过
            }
            Object raw = entry.getValue();
            String value = raw == null ? "" : String.valueOf(raw).trim();
            if (value.isEmpty() && !op.isValueless()) {
                continue; // 空值条件跳过
            }
            SearchFilter filter;
            // OR 分支：v1||OP2_field2||v2
            if (value.contains("||")) {
                String[] segs = value.split("\\|\\|");
                if (segs.length >= 2) {
                    String[] orParts = segs[1].split("_", 2);
                    if (orParts.length == 2) {
                        try {
                            Operator orOp = Operator.valueOf(orParts[0]);
                            String orVal = segs.length > 2 ? segs[2].trim() : "";
                            if (!orVal.isEmpty() || orOp.isValueless()) {
                                String firstVal = op.isValueless() ? null : segs[0].trim();
                                filters.add(SearchFilter.or(
                                        new SearchFilter(parts[1], op, firstVal),
                                        new SearchFilter(orParts[1], orOp, orVal.isEmpty() ? null : orVal)));
                                continue;
                            }
                        } catch (IllegalArgumentException ignored) {
                            // OR 分支非法：按普通条件处理
                        }
                    }
                }
            }
            filter = new SearchFilter(parts[1], op, value.isEmpty() ? null : value);
            filters.add(filter);
        }
        return filters;
    }

    // ==================== 原生 SQL（对齐 db-spring doNativeQuery） ====================

    /**
     * 原生 SQL 查询（结果按驼峰属性映射到 returnType，需有对应 setter）。
     */
    public static <T> List<T> doNativeQuery(String sql, Map<String, Object> params, Class<T> returnType) {
        return doNativeQuery(sql, params, null, returnType);
    }

    /**
     * 原生 SQL 分页查询（LIMIT/OFFSET 语法，适用 MySQL/H2/PG；Oracle 等需自行改写 SQL）。
     */
    public static <T> List<T> doNativeQuery(String sql, Map<String, Object> params,
                                            Pageable pageable, Class<T> returnType) {
        NamedParameterJdbcTemplate jdbc = namedJdbc;
        if (jdbc == null) {
            throw new IllegalStateException("原生 SQL 查询需要 spring-jdbc 环境（未检测到 NamedParameterJdbcTemplate）");
        }
        Map<String, Object> bind = params == null ? new java.util.HashMap<>() : new java.util.HashMap<>(params);
        String execSql = sql;
        if (isPaged(pageable)) {
            execSql = sql + " LIMIT :__limit OFFSET :__offset";
            bind.put("__limit", pageable.getPageSize());
            bind.put("__offset", pageable.getOffset());
        }
        return jdbc.query(execSql, bind, BeanPropertyRowMapper.newInstance(returnType));
    }

    /**
     * 原生 SQL 计数：{@code SELECT COUNT(*) FROM (sql)}。
     */
    public static long doNativeCount(String sql, Map<String, Object> params) {
        NamedParameterJdbcTemplate jdbc = namedJdbc;
        if (jdbc == null) {
            throw new IllegalStateException("原生 SQL 查询需要 spring-jdbc 环境（未检测到 NamedParameterJdbcTemplate）");
        }
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM (" + sql + ") __scaffold_cnt",
                params == null ? new java.util.HashMap<>() : params, Long.class);
        return count == null ? 0L : count;
    }

    // ==================== 内部实现 ====================

    /**
     * 构建参数：固定条件（@Table.filters）+ 动态条件 + OR 条件 + 分页 + 排序。
     */
    private static MapBuilder builder(Class<?> beanClass, List<SearchFilter> filters,
                                      List<SearchFilter> orFilters, Pageable pageable, Sort sort) {
        MapBuilder mb = MapUtils.builder();
        FilterParams.applyAll(mb, fixedFilters(beanClass));
        FilterParams.applyAll(mb, filters);
        if (orFilters != null && !orFilters.isEmpty()) {
            FilterParams.apply(mb, SearchFilter.or(orFilters.toArray(new SearchFilter[0])));
        }
        if (isPaged(pageable)) {
            mb.page(pageable.getPageNumber(), pageable.getPageSize());
        }
        if (sort != null && sort.isSorted()) {
            applySort(mb, sort);
        }
        return mb;
    }

    private static MapBuilder builder(Class<?> beanClass, List<SearchFilter> filters, Pageable pageable) {
        return builder(beanClass, filters, null, pageable, pageable == null ? null : pageable.getSort());
    }

    private static void applySort(MapBuilder mb, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }
        for (Sort.Order order : sort) {
            mb.orderBy(order.getProperty(), order.isDescending() ? "desc" : "asc");
        }
    }

    private static boolean isPaged(Pageable pageable) {
        return pageable != null && pageable.isPaged();
    }

    private static boolean isPlanWithReturnType(Class<?> beanClass) {
        QueryPlan plan = beanClass.getAnnotation(QueryPlan.class);
        return plan != null && plan.returnType() != Void.class && plan.returnType() != beanClass;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> convertPlanRows(Class<?> planClass, List<Map<String, Object>> rows) {
        QueryPlan plan = planClass.getAnnotation(QueryPlan.class);
        Class<?> returnType = plan == null ? planClass : plan.returnType();
        if (returnType == Void.class || returnType == planClass) {
            return (List<T>) rows;
        }
        List<T> results = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            results.add((T) OBJECT_MAPPER.convertValue(row, returnType));
        }
        return results;
    }

    /**
     * 计划类上声明的固定条件（@Table.filters），总是生效。
     */
    static List<SearchFilter> fixedFilters(Class<?> beanClass) {
        Table table = beanClass.getAnnotation(Table.class);
        if (table == null || table.filters().length == 0) {
            return null;
        }
        List<SearchFilter> filters = new ArrayList<>();
        for (com.scaffold.search.plan.annotation.SearchFilter f : table.filters()) {
            filters.add(new SearchFilter(f.name(), f.op(), parseFixedValue(f.value(), f.type())));
        }
        return filters;
    }

    private static Object parseFixedValue(String raw, Class<?> type) {
        if (raw == null || raw.isEmpty() || String.class == type) {
            return raw;
        }
        if (type == Long.class || type == long.class) {
            return Long.parseLong(raw);
        }
        if (type == Integer.class || type == int.class) {
            return Integer.parseInt(raw);
        }
        if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(raw);
        }
        if (type == Double.class || type == double.class) {
            return Double.parseDouble(raw);
        }
        return raw;
    }
}
