package com.scaffold.search;

import cn.zhxu.bs.util.MapBuilder;
import com.scaffold.search.filter.Operator;
import com.scaffold.search.filter.SearchFilter;

import java.util.Collection;
import java.util.List;

/**
 * SearchFilter → Bean Searcher 参数 转换器（内部工具）。
 *
 * @author lianghang
 */
final class FilterParams {

    private FilterParams() {
    }

    /**
     * 将过滤器列表追加到 MapBuilder（AND 关系）。
     */
    static void applyAll(MapBuilder mb, List<SearchFilter> filters) {
        if (filters == null) {
            return;
        }
        for (SearchFilter filter : filters) {
            apply(mb, filter);
        }
    }

    /**
     * 追加单个过滤器：OR 分组走 MapBuilder.or(...)，普通条件走 field(...).op(...)。
     */
    static void apply(MapBuilder mb, SearchFilter filter) {
        if (filter == null) {
            return;
        }
        if (filter.isOrGroup()) {
            mb.or(orBuilder -> {
                for (SearchFilter branch : filter.getOrBranches()) {
                    applyField(orBuilder, branch);
                }
            });
            return;
        }
        applyField(mb, filter);
    }

    private static void applyField(cn.zhxu.bs.util.Builder<?> b, SearchFilter filter) {
        Operator op = filter.getOperator();
        if (op.isValueless()) {
            // 无值运算符也必须带一个空串占位值，否则 FieldParam 会被引擎丢弃
            b.field(filter.getFieldName(), "");
        } else if (op == Operator.IN || op == Operator.NI || op == Operator.BT || op == Operator.NBT) {
            Object value = filter.getValue();
            if (value instanceof Collection) {
                b.field(filter.getFieldName(), (Collection<?>) value);
            } else {
                b.field(filter.getFieldName(), value);
            }
        } else {
            b.field(filter.getFieldName(), filter.getValue());
        }
        b.op(op.fieldOp());
    }
}
