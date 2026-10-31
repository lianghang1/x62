package com.scaffold.search.plan;

import cn.zhxu.bs.BeanMeta;
import cn.zhxu.bs.FieldMeta;
import cn.zhxu.bs.MetaResolver;
import cn.zhxu.bs.SqlSnippet;
import cn.zhxu.bs.bean.Cluster;
import cn.zhxu.bs.bean.DbType;
import cn.zhxu.bs.implement.DefaultDbTypeMapper;
import cn.zhxu.bs.implement.DefaultMetaResolver;
import com.scaffold.search.plan.annotation.Column;
import com.scaffold.search.plan.annotation.FieldType;
import com.scaffold.search.plan.annotation.IgnoreProperties;
import com.scaffold.search.plan.annotation.IgnoreProperty;
import com.scaffold.search.plan.annotation.JoinColumn;
import com.scaffold.search.plan.annotation.JoinTable;
import com.scaffold.search.plan.annotation.QueryPlan;
import com.scaffold.search.plan.annotation.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查询计划元数据解析器：把 {@code @QueryPlan + @Table + @JoinTable + @Column}
 * 注解在运行时合成为 Bean Searcher {@link BeanMeta}，SQL 由 Bean Searcher 统一生成；
 * 非计划类委托给默认解析器。
 *
 * <p>字段映射语义：未标注 {@code @Column} 的字段按驼峰转下划线自动映射
 * （主表别名前缀），{@code @IgnoreProperty / @IgnoreProperties} 排除，
 * {@code @FieldType / @FieldTypes} 覆盖字段数据类型，{@code useIndex} 生成 FORCE INDEX。
 *
 * <p>由 {@code SearchAutoConfiguration} 通过 BeanPostProcessor 挂到
 * BeanSearcher / MapSearcher 上。
 *
 * @author lianghang
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

        // 需要忽略的属性（@IgnoreProperty 字段级 + @IgnoreProperties 类级）
        Set<String> ignored = collectIgnored(planClass);
        // @FieldType 覆盖的字段类型
        Map<String, Class<?>> typeOverrides = collectTypeOverrides(planClass);

        BeanMeta<?> beanMeta = new BeanMeta<>(planClass, null,
                new SqlSnippet(buildTables(planClass, table, mainAlias)),
                new SqlSnippet(""), new SqlSnippet(""), new SqlSnippet(""), new SqlSnippet(""),
                true, false, 0, 0, 0);

        DefaultDbTypeMapper dbTypeMapper = new DefaultDbTypeMapper();
        int mapped = 0;
        for (Field field : allFields(planClass)) {
            String property = field.getName();
            if (ignored.contains(property)) {
                continue;
            }
            Column column = field.getAnnotation(Column.class);
            String prefix = column != null && !column.table().isEmpty() ? column.table() : mainAlias;
            String columnExpr;
            if (column != null && (!column.name().isEmpty() || !column.value().isEmpty())) {
                columnExpr = !column.name().isEmpty() ? column.name() : column.value();
            } else {
                // 自动映射：驼峰转下划线
                columnExpr = snakeCase(property);
            }
            String fieldSql = columnExpr.contains(".") || hasSqlKeyword(columnExpr)
                    ? columnExpr : prefix + "." + columnExpr;

            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Class<? extends cn.zhxu.bs.FieldOp>[] noOps = new Class[0];
            Class<?> declaredType = typeOverrides.getOrDefault(property, field.getType());
            DbType dbType = dbTypeMapper.map(declaredType);
            FieldMeta fieldMeta = new FieldMeta(beanMeta, property, field,
                    new SqlSnippet(fieldSql), property, true, noOps, dbType, Cluster.AUTO);
            beanMeta.addFieldMeta(fieldMeta);
            mapped++;
        }
        if (mapped == 0) {
            throw new IllegalStateException("QueryPlan [" + planClass.getName()
                    + "] 没有任何可映射字段，请检查 @Column / @IgnoreProperty 配置");
        }
        return beanMeta;
    }

    /**
     * 主表 + 关联表 → Bean Searcher tables 片段：
     * {@code t_order o FORCE INDEX(idx_xxx) left join t_dept d on (d.dept_id = o.id)}
     */
    private String buildTables(Class<?> planClass, Table table, String mainAlias) {
        StringBuilder tables = new StringBuilder(table.name());
        if (!table.alias().isEmpty()) {
            tables.append(' ').append(table.alias());
        }
        if (!table.useIndex().isEmpty()) {
            tables.append(" FORCE INDEX(").append(table.useIndex()).append(')');
        }
        for (JoinTable joinTable : planClass.getAnnotationsByType(JoinTable.class)) {
            String joinTableName = joinTable.schema().isEmpty()
                    ? joinTable.name() : joinTable.schema() + "." + joinTable.name();
            tables.append(' ').append(joinTable.joinType().trim())
                    .append(' ').append(joinTableName)
                    .append(' ').append(joinTable.alias());
            if (!joinTable.useIndex().isEmpty()) {
                tables.append(" FORCE INDEX(").append(joinTable.useIndex()).append(')');
            }
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

    private Set<String> collectIgnored(Class<?> planClass) {
        Set<String> ignored = new HashSet<>();
        for (Field field : planClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(IgnoreProperty.class)) {
                ignored.add(field.getName());
            }
        }
        IgnoreProperties properties = planClass.getAnnotation(IgnoreProperties.class);
        if (properties != null) {
            for (IgnoreProperty p : properties.value()) {
                ignored.add(p.name());
            }
        }
        return ignored;
    }

    private Map<String, Class<?>> collectTypeOverrides(Class<?> planClass) {
        Map<String, Class<?>> overrides = new HashMap<>();
        FieldType[] fieldTypes = planClass.getAnnotationsByType(FieldType.class);
        for (FieldType fieldType : fieldTypes) {
            overrides.put(fieldType.name(), fieldType.type());
        }
        return overrides;
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
        String lower = expr.toLowerCase(Locale.ROOT);
        return lower.contains("count(") || lower.contains("sum(") || lower.contains("max(")
                || lower.contains("min(") || lower.contains("avg(") || lower.contains(" ");
    }

    /** 驼峰转下划线：userName → user_name */
    static String snakeCase(String property) {
        StringBuilder sb = new StringBuilder();
        for (char c : property.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
