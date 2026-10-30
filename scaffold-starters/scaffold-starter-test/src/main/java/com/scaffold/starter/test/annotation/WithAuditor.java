package com.scaffold.starter.test.annotation;

import com.scaffold.starter.test.extension.AuditorExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为测试注入 JPA 审计人：测试方法执行期间把指定账户写入
 * {@code ScaffoldAuditorHolder}，结束后自动清理，用于断言 createBy/updateBy。
 *
 * <p>支持类级与方法级，方法级优先。等价的手工写法是在测试内直接调用
 * {@code ScaffoldAuditorHolder.set(...)/clear(...)}，本注解只是消除该样板。</p>
 *
 * @author lianghang
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(AuditorExtension.class)
public @interface WithAuditor {

    /** 注入的审计人标识，默认 tester。 */
    String value() default "tester";
}
