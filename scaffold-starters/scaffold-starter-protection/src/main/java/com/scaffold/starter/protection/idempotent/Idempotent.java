package com.scaffold.starter.protection.idempotent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等注解：以「账户/设备 + URI + 参数摘要」为幂等键，窗口期内重复提交直接拒绝（1409）。
 * 适用防重复下单/重复打款等场景；状态机幂等请用 @Version 乐观锁。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等窗口（秒） */
    int windowSeconds() default 10;
}
