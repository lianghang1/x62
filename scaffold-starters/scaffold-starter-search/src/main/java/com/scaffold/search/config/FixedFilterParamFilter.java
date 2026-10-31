package com.scaffold.search.config;

import cn.zhxu.bs.BeanMeta;
import cn.zhxu.bs.ParamFilter;
import com.scaffold.search.plan.annotation.SearchFilter;
import com.scaffold.search.plan.annotation.Table;

import java.util.HashMap;
import java.util.Map;

/**
 * 固定条件参数过滤器：把计划类（@QueryPlan + @Table）上声明的
 * {@link Table#filters()} 注入检索参数，并<strong>丢弃</strong>同字段的
 * 动态参数（值、运算符、忽略大小写、数组等），保证固定条件总是生效、
 * 前端无法覆盖。
 *
 * <p>对三种用法统一生效：QueryUtils 编程式、按名/按类计划查询、
 * 前端参数直通（用户自行注入 BeanSearcher/MapSearcher）。
 *
 * <p>注意：同字段冲突的语义是「固定条件优先，动态条件被静默丢弃」——
 * 若需要同字段多条件（如 status IN (...) AND status &gt; 2），
 * 应在计划类设计上避免把该字段声明为固定条件。
 */
public class FixedFilterParamFilter implements ParamFilter {

    @Override
    public <T> Map<String, Object> doFilter(BeanMeta<T> beanMeta, Map<String, Object> paraMap) {
        Table table = beanMeta.getBeanClass().getAnnotation(Table.class);
        if (table == null || table.filters().length == 0) {
            return paraMap;
        }
        Map<String, Object> params = new HashMap<>(paraMap);
        for (SearchFilter f : table.filters()) {
            String name = f.name();
            // 丢弃同字段的动态参数：FieldParam 对象（MapBuilder 构建）、值、运算符、忽略大小写、数组/区间子项
            params.entrySet().removeIf(e -> {
                if (e.getValue() instanceof cn.zhxu.bs.param.FieldParam) {
                    return name.equals(((cn.zhxu.bs.param.FieldParam) e.getValue()).getName());
                }
                String key = e.getKey();
                return key.equals(name) || key.equals(name + "_op") || key.equals(name + "_ic")
                        || key.startsWith(name + "[") || key.startsWith(name + "_");
            });
            // 注入固定条件
            if (!f.op().isValueless()) {
                params.put(name, FixedValues.parse(f.value(), f.type()));
            }
            params.put(name + "_op", f.op().fieldOp());
        }
        return params;
    }

    /** 固定条件值解析（字符串 → 声明类型） */
    static final class FixedValues {

        private FixedValues() {
        }

        static Object parse(String raw, Class<?> type) {
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
            if (type == Float.class || type == float.class) {
                return Float.parseFloat(raw);
            }
            if (type == Short.class || type == short.class) {
                return Short.parseShort(raw);
            }
            if (type == java.math.BigDecimal.class) {
                return new java.math.BigDecimal(raw);
            }
            throw new IllegalArgumentException("不支持的固定条件类型: " + type.getName());
        }
    }
}
