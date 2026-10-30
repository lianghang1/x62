package com.scaffold.starter.protection.idempotent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等注解，窗口期内拒绝重复提交。
 *
 * <p>以「账户/设备 + URI + 参数摘要」为幂等键，重复请求直接拒绝（1409）；
 * 适用于防重复下单、重复打款等场景——状态机幂等请用 @Version 乐观锁，二者不混用。</p>
 *
 * @author lianghang
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等窗口（秒） */
    int windowSeconds() default 10;
}
