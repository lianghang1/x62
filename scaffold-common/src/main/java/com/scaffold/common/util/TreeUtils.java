package com.scaffold.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 树结构装配工具：扁平 List → 树形 List 的通用出口。
 *
 * <p>菜单、部门、地区、分类等「自引用表」的通用装配器——不绑定任何实体，
 * 通过函数式参数取 id / parentId / 挂载 children，实体零侵入：</p>
 *
 * <pre>{@code
 * List<MenuNode> tree = TreeUtils.build(
 *         menuList,
 *         MenuNode::getId,
 *         MenuNode::getParentId,
 *         0L,                          // 根节点的 parentId 约定
 *         MenuNode::setChildren);
 * }</pre>
 *
 * <p>装配语义：</p>
 * <ul>
 *   <li>parentId 等于 rootParentId（含双方为 null）→ 根节点；</li>
 *   <li>parentId 指向不存在的节点（脏数据/父级被删）→ 按「孤儿提升为根」处理，
 *       宁可多展示不可静默丢数据；</li>
 *   <li>id 重复 → fail-fast 抛 {@link IllegalStateException}（装配结果不可信，尽早暴露）；</li>
 *   <li>叶子节点 children 恒为空 List（非 null），前端无需判空；</li>
 *   <li>同一层内保持入参顺序（数据库按 sort 排好传入即可）。</li>
 * </ul>
 *
 * <p>单趟分组实现，O(n)；不检测环——环会表现为「该子树从根上消失」而非死循环，
 * 由「孤儿提升」语义兜底可见性。</p>
 *
 * @author lianghang
 */
public final class TreeUtils {

    private TreeUtils() {
    }

    /**
     * 扁平列表装配为树。
     *
     * @param nodes          扁平节点列表（同层顺序即结果顺序），允许 null/空
     * @param idGetter       取节点主 id
     * @param parentIdGetter 取父 id
     * @param rootParentId   根节点的 parentId 约定值（可为 null）
     * @param childrenSetter 挂载 children 的写入口（实体 setter 或 Map.put）
     * @param <T>            节点类型
     * @param <ID>           id 类型（Long/String 均可，需正确实现 equals/hashCode）
     * @return 根节点列表（装配后 children 已逐层挂好）；入参为空返回空 List
     */
    public static <T, ID> List<T> build(List<T> nodes,
                                        Function<T, ID> idGetter,
                                        Function<T, ID> parentIdGetter,
                                        ID rootParentId,
                                        BiConsumer<T, List<T>> childrenSetter) {
        List<T> roots = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return roots;
        }

        // 第一趟：id 索引 + 重复 fail-fast
        Map<ID, T> idIndex = new HashMap<>(nodes.size() * 2);
        for (T node : nodes) {
            ID id = idGetter.apply(node);
            T previous = idIndex.put(id, node);
            if (previous != null) {
                throw new IllegalStateException("TreeUtils: 节点 id 重复, id=" + id);
            }
        }

        // 第二趟：按 parentId 分组；根 / 孤儿 → roots
        Map<ID, List<T>> childrenByParentId = new HashMap<>();
        for (T node : nodes) {
            ID parentId = parentIdGetter.apply(node);
            if (isRoot(parentId, rootParentId) || !idIndex.containsKey(parentId)) {
                roots.add(node);
            } else {
                childrenByParentId.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
            }
        }

        // 第三趟：统一回写 children（叶子给空 List，非 null）
        for (T node : nodes) {
            List<T> children = childrenByParentId.get(idGetter.apply(node));
            childrenSetter.accept(node, children != null ? children : new ArrayList<>());
        }
        return roots;
    }

    /** parentId == rootParentId（含双方 null）视为根。 */
    private static <ID> boolean isRoot(ID parentId, ID rootParentId) {
        return Objects.equals(parentId, rootParentId);
    }
}
