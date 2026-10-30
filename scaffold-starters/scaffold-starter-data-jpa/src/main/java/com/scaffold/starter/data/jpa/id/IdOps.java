package com.scaffold.starter.data.jpa.id;

import com.scaffold.common.util.SpringContextHolder;

/**
 * 雪花 ID 静态门面：非注入场景取号的统一出口。
 *
 * <p>委托容器内的 {@link Snowflake} 单例（data-jpa 自动装配注册），与 JPA 主键
 * {@code SnowflakeIdentifierGenerator} 共用同一节点标识与序列——绝不能另起实例，
 * 否则多实例部署下会绕开 workerId 约束生成重复 ID。典型用途：对外单号、
 * 缓存键去重、消息幂等号等主键之外的取号场景。</p>
 *
 * @author lianghang
 */
public final class IdOps {

    private IdOps() {
    }

    /** 生成全局唯一、趋势递增的 Long ID（线程安全）。 */
    public static long nextId() {
        return SpringContextHolder.getBean(Snowflake.class).nextId();
    }
}
