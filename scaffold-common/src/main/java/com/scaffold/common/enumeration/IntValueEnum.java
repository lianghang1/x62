package com.scaffold.common.enumeration;

/**
 * 值枚举通用接口：枚举有整数取值时的统一契约。
 *
 * <p>配合 {@code @ValidateEnum} 校验、前端字典下发、DB 存 int 值等场景——
 * 业务枚举实现本接口后，校验/序列化只认契约不认具体枚举。</p>
 *
 * @param <E> 枚举自身（自限定泛型）
 * @author lianghang
 */
public interface IntValueEnum<E extends Enum<E>> {

    /** 存库/传输用的整数值 */
    int getValue();

    /** 展示文案（前端字典/日志可读化用） */
    String getLabel();
}
