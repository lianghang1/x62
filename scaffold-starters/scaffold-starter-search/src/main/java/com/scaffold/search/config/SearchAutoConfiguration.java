package com.scaffold.search.config;

import cn.zhxu.bs.BeanSearcher;
import cn.zhxu.bs.MapSearcher;
import cn.zhxu.bs.boot.BeanSearcherAutoConfiguration;
import cn.zhxu.bs.implement.BaseSearcher;
import com.scaffold.search.QueryUtils;
import com.scaffold.search.plan.QueryPlanRegistry;
import com.scaffold.search.plan.ScaffoldMetaResolver;
import com.scaffold.search.plan.annotation.QueryPlan;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.Set;

/**
 * 动态检索自动配置：
 * <ol>
 *   <li>把 {@link ScaffoldMetaResolver} 挂到 BeanSearcher / MapSearcher（支持 QueryPlan 注解合成）</li>
 *   <li>初始化 {@link QueryUtils} 静态门面</li>
 *   <li>扫描并注册 QueryPlan 计划类</li>
 * </ol>
 *
 * <p>Bean Searcher starter 默认使用 SpringSqlExecutor，检索自动参与 Spring 事务。
 */
@AutoConfiguration(after = BeanSearcherAutoConfiguration.class)
@ConditionalOnProperty(name = "scaffold.search.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SearchProperties.class)
public class SearchAutoConfiguration {

    /**
     * 包装检索器的元数据解析器，使 @QueryPlan 注解类可被检索。
     */
    @Bean
    public static SearcherPostProcessor searcherPostProcessor() {
        return new SearcherPostProcessor();
    }

    @Bean
    public QueryUtilsInitializer queryUtilsInitializer(BeanSearcher beanSearcher, MapSearcher mapSearcher) {
        return new QueryUtilsInitializer(beanSearcher, mapSearcher);
    }

    @Bean
    public QueryPlanRegistrar queryPlanRegistrar(SearchProperties properties) {
        return new QueryPlanRegistrar(properties);
    }

    /**
     * 检索器后置处理器：替换 metaResolver 为合成版（保留原实现为 fallback）。
     */
    static class SearcherPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof BaseSearcher) {
                BaseSearcher searcher = (BaseSearcher) bean;
                searcher.setMetaResolver(new ScaffoldMetaResolver(searcher.getMetaResolver()));
            }
            return bean;
        }
    }

    /**
     * 初始化 QueryUtils 静态门面。
     */
    static class QueryUtilsInitializer {

        QueryUtilsInitializer(BeanSearcher beanSearcher, MapSearcher mapSearcher) {
            QueryUtils.configure(beanSearcher, mapSearcher);
        }
    }

    /**
     * 扫描 {@code scaffold.search.plan-packages}，注册 @QueryPlan 计划类。
     */
    static class QueryPlanRegistrar implements InitializingBean {

        private final SearchProperties properties;

        QueryPlanRegistrar(SearchProperties properties) {
            this.properties = properties;
        }

        @Override
        public void afterPropertiesSet() throws IOException {
            if (properties.getPlanPackages().isEmpty()) {
                return;
            }
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false) {
                        @Override
                        protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                            // 计划类是具体类而非组件（无 @Component），放宽默认判定
                            return definition.getMetadata().isIndependent();
                        }
                    };
            scanner.addIncludeFilter(new AnnotationTypeFilter(QueryPlan.class));
            for (String pkg : properties.getPlanPackages()) {
                Set<BeanDefinition> definitions = scanner.findCandidateComponents(pkg);
                for (BeanDefinition definition : definitions) {
                    Class<?> clazz = ClassUtils.resolveClassName(definition.getBeanClassName(), getClass().getClassLoader());
                    QueryPlanRegistry.register(clazz);
                }
            }
        }
    }
}
