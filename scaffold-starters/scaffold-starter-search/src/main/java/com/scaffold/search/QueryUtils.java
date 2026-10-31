package com.scaffold.search;

import cn.zhxu.bs.BeanSearcher;
import cn.zhxu.bs.MapSearcher;
import cn.zhxu.bs.SearchResult;
import cn.zhxu.bs.util.MapBuilder;
import cn.zhxu.bs.util.MapUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.search.filter.SearchFilter;
import com.scaffold.search.plan.QueryPlanRegistry;
import com.scaffold.search.plan.annotation.QueryPlan;
import com.scaffold.search.plan.annotation.Table;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static volatile BeanSearcher beanSearcher;
    private static volatile MapSearcher mapSearcher;

    private QueryUtils() {
    }

    /** 由自动配置调用，应用内不要使用 */
    public static void configure(BeanSearcher beanSearcher, MapSearcher mapSearcher) {
        QueryUtils.beanSearcher = beanSearcher;
        QueryUtils.mapSearcher = mapSearcher;
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

    // ==================== 内部实现 ====================

    /**
     * 构建参数：固定条件（@Table.filters）+ 动态条件 + 分页 + 排序。
     */
    private static MapBuilder builder(Class<?> beanClass, List<SearchFilter> filters, Pageable pageable) {
        MapBuilder mb = MapUtils.builder();
        FilterParams.applyAll(mb, fixedFilters(beanClass));
        FilterParams.applyAll(mb, filters);
        if (isPaged(pageable)) {
            mb.page(pageable.getPageNumber(), pageable.getPageSize());
            applySort(mb, pageable.getSort());
        }
        return mb;
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
        for (Table.Fixed f : table.filters()) {
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
