package com.scaffold.starter.data.jpa.id;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Hibernate 主键生成器，将主键生成委托给 {@code Snowflake} 单例。
 *
 * <p>实体上通过 {@code @GeneratedValue(generator = "snowflake")} 配合
 * {@code @GenericGenerator(name = "snowflake", strategy = "...SnowflakeIdentifierGenerator")}
 * 使用（见 {@code BaseEntity}）。</p>
 *
 * <p>实例由自动装配通过 {@code init} 静态注入——Hibernate 反射实例化生成器时
 * 拿不到 Spring 容器；未走装配的场景（如单元测试）则兜底构建默认实例。</p>
 *
 * @author lianghang
 */
public class SnowflakeIdentifierGenerator implements IdentifierGenerator {

    private static Snowflake snowflake;

    /** 由自动装配注入单例（避免 Hibernate 反射实例化时丢失 Spring 配置）。 */
    public static void init(Snowflake instance) {
        SnowflakeIdentifierGenerator.snowflake = instance;
    }

    @Override
    public java.io.Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        if (snowflake == null) {
            // 兜底：单元测试等未经过自动装配的场景
            synchronized (SnowflakeIdentifierGenerator.class) {
                if (snowflake == null) {
                    snowflake = new Snowflake(1, 1);
                }
            }
        }
        return snowflake.nextId();
    }
}
