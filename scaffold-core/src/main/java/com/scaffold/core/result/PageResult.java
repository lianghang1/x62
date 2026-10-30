package com.scaffold.core.result;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果。
 *
 * <p>与具体 ORM 解耦——JPA 的 {@code Page} 在 Repository 层转换为本类后再出 Service，
 * 避免框架类型沿调用链向上泄漏；空页场景经 {@code empty} 构造时列表恒不为 null。</p>
 *
 * @param <T> 数据类型
 * @author lianghang
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> list;

    /** 总条数 */
    private long total;

    /** 页码（从 1 开始） */
    private long pageNum;

    /** 每页大小 */
    private long pageSize;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, long pageNum, long pageSize) {
        this.list = list == null ? Collections.<T>emptyList() : list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        return new PageResult<>(list, total, pageNum, pageSize);
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(Collections.<T>emptyList(), 0, pageNum, pageSize);
    }

    // ---------- getter / setter ----------

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }
}
