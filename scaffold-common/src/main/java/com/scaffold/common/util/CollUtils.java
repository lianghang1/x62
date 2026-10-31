package com.scaffold.common.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 集合工具唯一入口：组合门面（非继承）——委托 Spring {@code CollectionUtils}
 * 与 guava {@code Sets/Lists/Immutable*}，只收脚手架内高频的 15 个方法。
 *
 * <p>为什么不继承：guava 的工具类全是 final + 私有构造，技术上不可继承；
 * 且集合底座有两个来源（Spring/guava），单继承也装不下。组合门面是集合域唯一可行的
 * 统一入口形态。import 唯一性由 ArchUnit 规则强制（{@code ScaffoldArchRules}）。</p>
 *
 * <p>使用边界：本类覆盖 80% 高频操作；guava 的高级结构
 * （BiMap/Multimap/Table/Range）直接 import guava 原生类使用，门面不转发。</p>
 *
 * @author lianghang
 */
public final class CollUtils {

    private CollUtils() {
    }

    // ---------- 判空（委托 org.springframework.util.CollectionUtils 语义） ----------

    /** null 或空集合 → true */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /** 非 null 且非空 → true */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /** null 或空 Map → true */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /** 非 null 且非空 Map → true */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /** null 安全：null 返回空集合，否则原样返回 */
    public static <T> Collection<T> emptyIfNull(Collection<T> collection) {
        return collection == null ? java.util.Collections.emptyList() : collection;
    }

    // ---------- 集合运算（委托 guava Sets） ----------

    /** 交集：both a 和 b 都有的元素 */
    public static <E> Set<E> intersection(Set<E> a, Set<E> b) {
        return Sets.intersection(a, b);
    }

    /** 并集：a 或 b 的全部元素 */
    public static <E> Set<E> union(Set<E> a, Set<E> b) {
        return Sets.union(a, b);
    }

    /** 差集：在 a 中但不在 b 中 */
    public static <E> Set<E> difference(Set<E> a, Set<E> b) {
        return Sets.difference(a, b);
    }

    // ---------- 分批（委托 guava Lists） ----------

    /** 大列表按 size 分批（批量入库/分页调用标配），size 必须 &gt; 0 */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("CollUtils: size 必须 > 0, 当前=" + size);
        }
        return Lists.partition(list, size);
    }

    // ---------- 不可变集合（委托 guava Immutable*） ----------

    /** 不可变 List：null/空 → 空不可变集合；防御配置表/常量集合被误改 */
    public static <T> List<T> immutableList(Collection<T> collection) {
        return collection == null || collection.isEmpty()
                ? ImmutableList.of() : ImmutableList.copyOf(collection);
    }

    /** 不可变 Set：null/空 → 空不可变集合 */
    public static <T> Set<T> immutableSet(Collection<T> collection) {
        return collection == null || collection.isEmpty()
                ? ImmutableSet.of() : ImmutableSet.copyOf(collection);
    }

    /** 不可变 Map：null/空 → 空不可变集合 */
    public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
        return map == null || map.isEmpty()
                ? ImmutableMap.of() : ImmutableMap.copyOf(map);
    }
}
