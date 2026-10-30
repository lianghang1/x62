package com.scaffold.starter.test.annotation;

import com.scaffold.starter.web.config.WebAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Web 测试切片：只装配「被测 Controller + scaffold-web 横切层」的轻量 MockMvc 环境。
 *
 * <p>比 {@code @SpringBootTest} 全量启动快一个量级，且不需要数据库/Redis：
 * 通过 {@link ImportAutoConfiguration} 精确导入 starter-web 的自动装配，
 * 因此被测接口天然获得与生产一致的行为——traceId 响应头、XSS 过滤、
 * 统一异常响应（业务码双轨）、Long 转字符串等 Jackson 规约，无需再手写
 * {@code MockMvcBuilders.standaloneSetup} 样板。</p>
 *
 * <p>用法：{@code @ScaffoldWebSlice(controllers = DemoController.class)}，
 * 测试类内直接 {@code @Autowired MockMvc}。需要测试类可发现的
 * {@code @SpringBootConfiguration}（业务方即应用主类，测试模块内用嵌套静态类）。</p>
 *
 * @author lianghang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@WebMvcTest
@ImportAutoConfiguration(WebAutoConfiguration.class)
public @interface ScaffoldWebSlice {

    /**
     * 被测 Controller，语义与 {@link WebMvcTest#controllers()} 一致；
     * 留空时扫描测试包下全部 {@code @Controller}。
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] controllers() default {};
}
