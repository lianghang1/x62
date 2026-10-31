package com.scaffold.search.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 检索过滤器，底层转成 Bean Searcher 检索参数。
 *
 * <pre>{@code
 * List<SearchFilter> filters = new ArrayList<>();
 * filters.add(new SearchFilter("status", Operator.EQ, 1));
 * filters.add(new SearchFilter(UserVO::getName, Operator.CN, "张"));   // lambda 字段
 * filters.add(SearchFilter.or(                                        // OR 分支
 *         new SearchFilter("deptId", Operator.EQ, 1L),
 *         new SearchFilter("deptId", Operator.EQ, 2L)));
 * }</pre>
 *
 * @author lianghang
 */
public class SearchFilter {

    /** 字段名（属性名，非数据库列名） */
    private final String fieldName;
    /** 运算符 */
    private final Operator operator;
    /** 字段值 */
    private final Object value;
    /** OR 分组：非 null 时本对象代表一组 OR 分支 */
    private final List<SearchFilter> orBranches;

    public SearchFilter(String fieldName, Operator operator, Object value) {
        this.fieldName = fieldName;
        this.operator = operator;
        this.value = value;
        this.orBranches = null;
    }

    /**
     * 无值运算符专用（IS NULL / IS NOT NULL / 空串判断）。
     */
    public SearchFilter(String fieldName, Operator operator) {
        if (!operator.isValueless()) {
            throw new IllegalArgumentException("运算符 " + operator + " 需要传值，请使用三参构造");
        }
        this.fieldName = fieldName;
        this.operator = operator;
        this.value = null;
        this.orBranches = null;
    }

    /**
     * lambda 字段引用构造（编译期安全）。
     */
    public <T> SearchFilter(SFunction<T, ?> field, Operator operator, Object value) {
        this(LambdaUtils.propertyName(field), operator, value);
    }

    private SearchFilter(List<SearchFilter> orBranches) {
        this.fieldName = null;
        this.operator = null;
        this.value = null;
        this.orBranches = Collections.unmodifiableList(orBranches);
    }

    /**
     * 构造 OR 分组：各分支之间 OR，分支内为单个条件。
     */
    public static SearchFilter or(SearchFilter... branches) {
        if (branches == null || branches.length == 0) {
            throw new IllegalArgumentException("or(...) 至少需要一个分支");
        }
        return new SearchFilter(Arrays.asList(branches));
    }

    public String getFieldName() {
        return fieldName;
    }

    public Operator getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    public boolean isOrGroup() {
        return orBranches != null;
    }

    public List<SearchFilter> getOrBranches() {
        return orBranches;
    }

    @Override
    public String toString() {
        if (isOrGroup()) {
            return "(" + orBranches + " OR)";
        }
        return fieldName + " " + operator + " " + value;
    }
}
