package com.scaffold.common;

import com.scaffold.common.util.CollUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CollUtils 组合门面测试：判空/集合运算/分批/不可变集合。
 *
 * @author lianghang
 */
class CollUtilsTest {

    @Test
    void emptyChecks() {
        assertTrue(CollUtils.isEmpty((List<?>) null));
        assertTrue(CollUtils.isEmpty(new ArrayList<>()));
        assertTrue(CollUtils.isNotEmpty(Arrays.asList("a")));
        assertTrue(CollUtils.isEmpty((Map<?, ?>) null));
        Map<String, Integer> nonEmpty = new HashMap<>();
        nonEmpty.put("k", 1);
        assertFalse(CollUtils.isEmpty(nonEmpty));
        assertTrue(CollUtils.isNotEmpty(nonEmpty));
        // emptyIfNull
        assertTrue(CollUtils.emptyIfNull(null).isEmpty());
        assertEquals(1, CollUtils.emptyIfNull(Arrays.asList("a")).size());
    }

    @Test
    void setOperations() {
        Set<String> a = new HashSet<>(Arrays.asList("x", "y", "z"));
        Set<String> b = new HashSet<>(Arrays.asList("y", "w"));

        assertEquals(new HashSet<>(Arrays.asList("y")), CollUtils.intersection(a, b));
        assertEquals(new HashSet<>(Arrays.asList("x", "y", "z", "w")), CollUtils.union(a, b));
        assertEquals(new HashSet<>(Arrays.asList("x", "z")), CollUtils.difference(a, b));
    }

    @Test
    void partitionBatches() {
        List<Integer> big = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> batches = CollUtils.partition(big, 2);
        assertEquals(3, batches.size());
        assertEquals(Arrays.asList(1, 2), batches.get(0));
        assertEquals(Arrays.asList(5), batches.get(2));
        assertThrows(IllegalArgumentException.class, () -> CollUtils.partition(big, 0));
    }

    @Test
    void immutableCollections() {
        assertTrue(CollUtils.immutableList(null).isEmpty());
        assertTrue(CollUtils.immutableSet(new ArrayList<>()).isEmpty());
        assertTrue(CollUtils.immutableMap(null).isEmpty());

        List<String> mutable = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> frozen = CollUtils.immutableList(mutable);
        assertEquals(Arrays.asList("a", "b"), frozen);
        assertThrows(UnsupportedOperationException.class, () -> frozen.add("c"));
        // 源列表后续修改不影响冻结副本
        mutable.add("c");
        assertEquals(2, frozen.size());

        Map<String, Integer> source = new HashMap<>();
        source.put("k", 1);
        Map<String, Integer> frozenMap = CollUtils.immutableMap(source);
        assertEquals(1, frozenMap.get("k"));
        assertThrows(UnsupportedOperationException.class, () -> frozenMap.put("x", 2));
    }
}
