package com.scaffold.starter.data.jpa.id;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Hibernate 主键生成器：委托 Snowflake 生成 Long 主键。
 * 实体上使用：@GeneratedValue(generator = "snowflake") + @GenericGenerator(name = "snowflake", strategy = "...SnowflakeIdentifierGenerator")
 */
public class SnowflakeIdentifierGenerator implements IdentifierGenerator {

    private static Snowflake snowflake;

    /** 由自动装配注入单例（避免 Hibernate 反射实例化时丢失 Spring 配置） */
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
