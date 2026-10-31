package com.scaffold.search.filter;

import java.io.Serializable;

/**
 * 字段引用函数（用于 lambda 方式声明检索字段，编译期安全）。
 *
 * <p>用法：{@code new SearchFilter(UserVO::getStatus, Operator.EQ, 1)}
 *
 * <p>lambda 必须直接引用目标类的方法（如 {@code UserVO::getStatus}），
 * 不能包装在另一个 lambda 里，否则无法解析出字段名。
 *
 * @param <T> 实体/检索 Bean 类型
 * @param <R> 字段类型
 *
 * @author lianghang
 */
@FunctionalInterface
public interface SFunction<T, R> extends Serializable {

    R apply(T source);
}
