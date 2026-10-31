package com.scaffold.starter.data.jpa.datasource;

import com.scaffold.starter.data.jpa.config.DynamicDataSourceProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;



/**
 * {@link TargetDataSource} 切面：进入方法前把目标数据源 key 压栈，退出后弹栈恢复。
 *
 * <p><b>order 必须是最高优先级</b>——数据源切换必须发生在事务拦截器开启事务、
 * 拿到连接之前，否则连接已绑定到默认库、注解形同虚设。这是本组件唯一的硬性时序约束。</p>
 *
 * <p>key 在进入方法体之前与 {@code scaffold.datasource.dynamic.datasources} 配置比对，
 * 不存在直接抛 {@link IllegalStateException}——fail-fast，而非静默回落主库掩盖配置错误。</p>
 *
 * @author lianghang
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DynamicDataSourceAspect {

    private final DynamicDataSourceProperties properties;

    public DynamicDataSourceAspect(DynamicDataSourceProperties properties) {
        this.properties = properties;
    }

    @Pointcut("@within(com.scaffold.starter.data.jpa.datasource.TargetDataSource)"
            + " || @annotation(com.scaffold.starter.data.jpa.datasource.TargetDataSource)")
    public void targetDataSourcePointcut() {
    }

    @Around("targetDataSourcePointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        TargetDataSource target = resolve(point);
        if (target != null) {
            String key = target.value();
            if (!properties.getDatasources().containsKey(key)) {
                throw new IllegalStateException(
                        "数据源 [" + key + "] 未在 scaffold.datasource.dynamic.datasources 中配置，可用: "
                                + properties.getDatasources().keySet());
            }
            DataSourceContextHolder.push(key);
            try {
                return point.proceed();
            } finally {
                DataSourceContextHolder.pop();
            }
        }
        return point.proceed();
    }

    /**
     * 方法标注优先，其次目标类标注（方法声明类在父类、而子类整体标注时也能命中）。
     */
    private TargetDataSource resolve(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        TargetDataSource target = signature.getMethod().getAnnotation(TargetDataSource.class);
        if (target == null) {
            target = point.getTarget().getClass().getAnnotation(TargetDataSource.class);
        }
        return target;
    }
}
