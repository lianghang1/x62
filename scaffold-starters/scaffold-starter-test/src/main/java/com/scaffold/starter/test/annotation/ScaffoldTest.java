package com.scaffold.starter.test.annotation;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 脚手架测试基座注解，替代裸写 {@code @SpringBootTest}。
 *
 * <p>当前为组合入口并预留统一扩展点：后续所有框架级测试约定（如统一的
 * test profile 属性注入、测试期日志规约）都收敛到这一个注解上，业务方
 * 无需感知内部变化。JUnit5 平台经 {@link SpringExtension} 驱动。</p>
 *
 * <p>用法：标注在测试类上，与常规 {@code @SpringBootTest} 完全同构，
 * 需要一个可发现的 {@code @SpringBootConfiguration}（业务方即应用主类，
 * 测试模块内可用嵌套静态类）。</p>
 *
 * @author lianghang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(SpringExtension.class)
public @interface ScaffoldTest {
}
