package com.scaffold.common.config;

import com.scaffold.common.util.SpringContextHolder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * scaffold-common 自动装配：注册 {@link SpringContextHolder} 静态持有器。
 *
 * <p>EventOps/IdOps/DataCipherHolder 等静态出口都依赖它经容器回填，
 * 引入 scaffold-common 即自动生效，业务侧无需手工注册。</p>
 *
 * @author lianghang
 */
@AutoConfiguration
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }
}
