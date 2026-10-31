package com.scaffold.common;

import com.scaffold.common.util.TreeUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TreeUtils 装配语义测试：三层挂载 / 孤儿提升 / 重复 id fail-fast / 空入参。
 *
 * @author lianghang
 */
class TreeUtilsTest {

    /** 测试节点：公开字段直接当 setter 用 */
    static class Node {
        final Long id;
        Long parentId;
        List<Node> children;

        Node(Long id, Long parentId) {
            this.id = id;
            this.parentId = parentId;
        }

        void setChildren(List<Node> children) {
            this.children = children;
        }
    }

    @Test
    void buildThreeLevelTree() {
        List<Node> nodes = Arrays.asList(
                new Node(1L, 0L),
                new Node(2L, 0L),
                new Node(11L, 1L),
                new Node(12L, 1L),
                new Node(111L, 11L));

        List<Node> roots = TreeUtils.build(nodes, n -> n.id, n -> n.parentId, 0L, Node::setChildren);

        assertEquals(2, roots.size());
        Node root1 = roots.get(0);
        assertEquals(1L, root1.id);
        assertEquals(2, root1.children.size());
        assertEquals(11L, root1.children.get(0).id);
        // 第三层
        assertEquals(1, root1.children.get(0).children.size());
        assertEquals(111L, root1.children.get(0).children.get(0).id);
        // 叶子 children 恒为空 List 而非 null
        assertNotNull(root1.children.get(1).children);
        assertTrue(root1.children.get(1).children.isEmpty());
    }

    @Test
    void orphanPromotedToRoot() {
        // parentId=99 不存在 → 提升为根，不静默丢数据
        List<Node> nodes = Collections.singletonList(new Node(1L, 99L));
        List<Node> roots = TreeUtils.build(nodes, n -> n.id, n -> n.parentId, 0L, Node::setChildren);
        assertEquals(1, roots.size());
        assertEquals(1L, roots.get(0).id);
    }

    @Test
    void duplicateIdFailsFast() {
        List<Node> nodes = Arrays.asList(new Node(1L, 0L), new Node(1L, 0L));
        assertThrows(IllegalStateException.class,
                () -> TreeUtils.build(nodes, n -> n.id, n -> n.parentId, 0L, Node::setChildren));
    }

    @Test
    void emptyOrNullInput() {
        assertTrue(TreeUtils.build(null, n -> n.id, n -> n.parentId, 0L, Node::setChildren).isEmpty());
        assertTrue(TreeUtils.build(new ArrayList<Node>(), n -> n.id, n -> n.parentId, 0L, Node::setChildren).isEmpty());
    }

    @Test
    void nullParentIdTreatedAsRoot() {
        List<Node> nodes = Collections.singletonList(new Node(1L, null));
        List<Node> roots = TreeUtils.build(nodes, n -> n.id, n -> n.parentId, 0L, Node::setChildren);
        assertEquals(1, roots.size());
    }

    @Test
    void preservesInputOrderInSameLevel() {
        // 同层顺序 = 入参顺序（调用方按 sort 排好传入）
        List<Node> nodes = Arrays.asList(
                new Node(1L, 0L),
                new Node(3L, 0L),
                new Node(2L, 0L));
        List<Node> roots = TreeUtils.build(nodes, n -> n.id, n -> n.parentId, 0L, Node::setChildren);
        assertEquals(1L, roots.get(0).id);
        assertEquals(3L, roots.get(1).id);
        assertEquals(2L, roots.get(2).id);
    }
}
