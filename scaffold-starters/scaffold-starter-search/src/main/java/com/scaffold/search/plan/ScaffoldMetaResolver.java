package com.scaffold.search.plan;

import cn.zhxu.bs.BeanMeta;
import cn.zhxu.bs.FieldMeta;
import cn.zhxu.bs.MetaResolver;
import cn.zhxu.bs.SqlSnippet;
import cn.zhxu.bs.bean.Cluster;
import cn.zhxu.bs.implement.DefaultMetaResolver;
import com.scaffold.search.plan.annotation.Column;
import com.scaffold.search.plan.annotation.JoinColumn;
import com.scaffold.search.plan.annotation.JoinTable;
import com.scaffold.search.plan.annotation.QueryPlan;
import com.scaffold.search.plan.annotation.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查询计划元数据解析器：把 {@code @QueryPlan + @Table + @JoinTable + @Column}
 * 注解在运行时合成为 Bean Searcher {@link BeanMeta}，SQL 由 Bean Searcher 统一生成；
 * 非计划类委托给默认解析器。
 *
 * <p>由 {@code SearchAutoConfiguration} 通过 BeanPostProcessor 挂到
 * BeanSearcher / MapSearcher 上。
 */
public class ScaffoldMetaResolver implements MetaResolver {

    private final MetaResolver fallback;
    private final Map<Class<?>, BeanMeta<?>> cache = new ConcurrentHashMap<>();

    public ScaffoldMetaResolver(MetaResolver fallback) {
        this.fallback = fallback;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> BeanMeta<T> resolve(Class<T> beanClass) {
        if (beanClass.isAnnotationPresent(QueryPlan.class) && beanClass.isAnnotationPresent(Table.class)) {
            return (BeanMeta<T>) cache.computeIfAbsent(beanClass, this::synthesize);
        }
        return fallback.resolve(beanClass);
    }

    @Override
    public void clearCache() {
        cache.clear();
        fallback.clearCache();
    }

    // ==================== 合成逻辑 ====================

    private BeanMeta<?> synthesize(Class<?> planClass) {
        Table table = planClass.getAnnotation(Table.class);
        String mainAlias = table.alias().isEmpty() ? table.name() : table.alias();

        BeanMeta<?> beanMeta = new BeanMeta<>(planClass, null,
                new SqlSnippet(buildTables(planClass, table, mainAlias)),
                new SqlSnippet(""), new SqlSnippet(""), new SqlSnippet(""), new SqlSnippet(""),
                true, false, 0, 0, 0);

        int mapped = 0;
        for (Field field : allFields(planClass)) {
            Column column = field.getAnnotation(Column.class);
            if (column == null) {
                continue;
            }
            String prefix = column.table().isEmpty() ? mainAlias : column.table();
            String columnExpr = !column.name().isEmpty() ? column.name()
                    : !column.value().isEmpty() ? column.value() : field.getName();
            String fieldSql = columnExpr.contains(".") || hasSqlKeyword(columnExpr)
                    ? columnExpr : prefix + "." + columnExpr;

            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Class<? extends cn.zhxu.bs.FieldOp>[] noOps = new Class[0];
            cn.zhxu.bs.bean.DbType dbType = new cn.zhxu.bs.implement.DefaultDbTypeMapper().map(field.getType());
            FieldMeta fieldMeta = new FieldMeta(beanMeta, field.getName(), field,
                    new SqlSnippet(fieldSql), field.getName(), true, noOps, dbType, Cluster.AUTO);
            beanMeta.addFieldMeta(fieldMeta);
            mapped++;
        }
        if (mapped == 0) {
            throw new IllegalStateException("QueryPlan [" + planClass.getName()
                    + "] 没有任何 @Column 字段映射，请至少标注一个字段");
        }
        return beanMeta;
    }

    /**
     * 主表 + 关联表 → Bean Searcher tables 片段：
     * {@code t_user u left join t_dept d on (d.dept_id = u.id)}
     */
    private String buildTables(Class<?> planClass, Table table, String mainAlias) {
        StringBuilder tables = new StringBuilder(table.name());
        if (!table.alias().isEmpty()) {
            tables.append(' ').append(table.alias());
        }
        for (JoinTable joinTable : planClass.getAnnotationsByType(JoinTable.class)) {
            String joinTableName = joinTable.schema().isEmpty()
                    ? joinTable.name() : joinTable.schema() + "." + joinTable.name();
            tables.append(' ').append(joinTable.joinType().trim())
                    .append(' ').append(joinTableName)
                    .append(' ').append(joinTable.alias());
            List<String> conditions = new ArrayList<>();
            for (JoinColumn jc : joinTable.columns()) {
                if (!jc.sql().isEmpty()) {
                    conditions.add("(" + jc.sql() + ")");
                } else if (!jc.referencedValue().isEmpty()) {
                    conditions.add("(" + joinTable.alias() + "." + jc.name() + " = '" + jc.referencedValue() + "')");
                } else {
                    conditions.add("(" + joinTable.alias() + "." + jc.name()
                            + " = " + mainAlias + "." + jc.referencedColumnName() + ")");
                }
            }
            if (!conditions.isEmpty()) {
                tables.append(" on ").append(String.join(" and ", conditions));
            }
        }
        return tables.toString();
    }

    private List<Field> allFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) {
                    continue;
                }
                fields.add(field);
            }
        }
        return fields;
    }

    private boolean hasSqlKeyword(String expr) {
        String lower = expr.toLowerCase();
        return lower.contains("count(") || lower.contains("sum(") || lower.contains("max(")
                || lower.contains("min(") || lower.contains("avg(") || lower.contains(" ");
    }
}
