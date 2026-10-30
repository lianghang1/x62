package com.scaffold.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

/**
 * Spring 上下文静态持有器：非 Spring Bean 场景取 Bean 的统一出口。
 *
 * <p>典型场景：JPA {@code AttributeConverter}、静态工厂、SPI 实现等容器无感对象
 * 需要读取 Bean 或配置时使用。<b>规约：Bean 能注入就不要用它</b>——
 * 静态入口是逃生门而非常规通道，滥用会让依赖关系不可见。</p>
 *
 * <p>由 Spring 自动回调（实现了 {@link ApplicationContextAware}，作为 Bean 注册后生效），
 * 容器未就绪时调用会抛出明确异常而非返回 null。</p>
 *
 * @author lianghang
 */
public class SpringContextHolder implements ApplicationContextAware {

    private static volatile ApplicationContext CONTEXT;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CONTEXT = applicationContext;
    }

    /** 按 Bean 名取 */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return (T) requireContext().getBean(name);
    }

    /** 按类型取 */
    public static <T> T getBean(Class<T> type) {
        return requireContext().getBean(type);
    }

    /** 按类型取全部实现（SPI 场景遍历用） */
    public static <T> java.util.Map<String, T> getBeansOfType(Class<T> type) {
        return requireContext().getBeansOfType(type);
    }

    /** 读环境配置 */
    public static String getProperty(String key) {
        return requireContext().getEnvironment().getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return requireContext().getEnvironment().getProperty(key, defaultValue);
    }

    /** 发布应用事件 */
    public static void publishEvent(Object event) {
        requireContext().publishEvent(event);
    }

    private static ApplicationContext requireContext() {
        ApplicationContext ctx = CONTEXT;
        if (ctx == null) {
            throw new IllegalStateException("SpringContextHolder 未就绪：ApplicationContext 尚未注入（容器未启动或类未被扫描）");
        }
        return ctx;
    }
}
