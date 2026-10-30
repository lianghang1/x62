package com.scaffold.module.system.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志：标注在 controller 写操作方法上，由 OperLogAspect 落库 sys_oper_log
 * （模块/动作/操作人/入参/耗时/结果）。查询类接口不必标注。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 所属业务模块，如「三方应用」「角色」 */
    String module();

    /** 动作描述，如「创建应用」「重置密钥」 */
    String action();
}
