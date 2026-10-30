package com.scaffold.starter.test.extension;

import com.scaffold.starter.data.jpa.audit.ScaffoldAuditorHolder;
import com.scaffold.starter.test.annotation.WithAuditor;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

/**
 * {@link WithAuditor} 的 JUnit 扩展：测试方法前写审计人、方法后清理。
 *
 * <p>类级与方法级注解均支持，方法级优先；兜底默认值 tester，保证单独标注
 * {@code @WithAuditor} 不带参数时语义明确。</p>
 *
 * @author lianghang
 */
public class AuditorExtension implements BeforeEachCallback, AfterEachCallback {

    private static final String KEY = "scaffoldAuditor";

    @Override
    public void beforeEach(ExtensionContext context) {
        WithAuditor annotation = resolve(context);
        String auditor = annotation == null ? "tester" : annotation.value();
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(KEY, auditor);
        ScaffoldAuditorHolder.set(auditor);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ScaffoldAuditorHolder.clear();
    }

    private WithAuditor resolve(ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        WithAuditor annotation = AnnotatedElementUtils.findMergedAnnotation(method, WithAuditor.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(context.getRequiredTestClass(), WithAuditor.class);
        }
        return annotation;
    }
}
