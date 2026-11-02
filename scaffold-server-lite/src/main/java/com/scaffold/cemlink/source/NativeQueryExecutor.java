package com.scaffold.cemlink.source;

import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 原生 SQL 结果集按列别名反射映射到 DTO，对应原采集程序 QueryUtils.doNativeQuery 模式
 *
 * @author lianghang
 */
public final class NativeQueryExecutor {

    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private NativeQueryExecutor() {
    }

    public static <T> List<T> query(JdbcTemplate jdbcTemplate, String sql, Class<T> type, Object... params) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            try {
                return mapRow(rs, type);
            } catch (Exception e) {
                throw new IllegalStateException("结果集映射失败: " + type.getSimpleName(), e);
            }
        }, params);
    }

    private static <T> T mapRow(ResultSet rs, Class<T> type) throws Exception {
        T bean = type.getDeclaredConstructor().newInstance();
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Field> fields = fieldsOf(type);
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            Field field = fields.get(camelCase(meta.getColumnLabel(i)));
            if (field != null) {
                Object value = convert(rs.getObject(i), field.getType());
                if (value != null) {
                    field.set(bean, value);
                }
            }
        }
        return bean;
    }

    private static Map<String, Field> fieldsOf(Class<?> type) {
        return FIELD_CACHE.computeIfAbsent(type, key -> {
            Map<String, Field> fields = new HashMap<>();
            for (Class<?> c = key; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    field.setAccessible(true);
                    fields.putIfAbsent(field.getName(), field);
                }
            }
            return fields;
        });
    }

    private static Object convert(Object raw, Class<?> target) {
        if (raw == null) {
            return null;
        }
        if (target == String.class) {
            return raw.toString();
        }
        if (target == Integer.class || target == int.class) {
            return raw instanceof Number ? ((Number) raw).intValue() : Integer.valueOf(raw.toString().trim());
        }
        if (target == Long.class || target == long.class) {
            return raw instanceof Number ? ((Number) raw).longValue() : Long.valueOf(raw.toString().trim());
        }
        if (target == Double.class || target == double.class) {
            return raw instanceof Number ? ((Number) raw).doubleValue() : Double.valueOf(raw.toString().trim());
        }
        if (target == BigDecimal.class) {
            return raw instanceof BigDecimal ? raw : new BigDecimal(raw.toString().trim());
        }
        if (target == Boolean.class || target == boolean.class) {
            if (raw instanceof Boolean) {
                return raw;
            }
            String v = raw.toString().trim();
            return "1".equals(v) || "true".equalsIgnoreCase(v) || "Y".equalsIgnoreCase(v);
        }
        if (target == java.util.Date.class) {
            if (raw instanceof Timestamp) {
                return new java.util.Date(((Timestamp) raw).getTime());
            }
            if (raw instanceof Date) {
                return new java.util.Date(((Date) raw).getTime());
            }
            if (raw instanceof java.util.Date) {
                return raw;
            }
        }
        return null;
    }

    private static String camelCase(String column) {
        StringBuilder sb = new StringBuilder(column.length());
        boolean upper = false;
        for (int i = 0; i < column.length(); i++) {
            char ch = column.charAt(i);
            if (ch == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
                upper = false;
            }
        }
        return sb.toString();
    }
}
