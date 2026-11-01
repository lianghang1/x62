package com.scaffold.starter.data.jpa.datachange;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据变更留痕标记：标注在实体类上，该实体 INSERT/UPDATE/DELETE 时
 * 自动生成变更事件（UPDATE 为字段级新旧值 diff）。
 *
 * <p>事件经事务提交后发布，由应用层异步落库（module-system 的
 * {@code sys_change_log}）；未标注的实体不产生任何开销。</p>
 *
 * @author lianghang
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataChange {

    /**
     * 业务对象名，写入留痕记录便于人读（如「账户」「角色」）；
     * 缺省为实体类简单名。
     */
    String value() default "";
}
