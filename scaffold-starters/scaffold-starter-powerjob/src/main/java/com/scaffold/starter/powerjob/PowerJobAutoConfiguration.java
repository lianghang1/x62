package com.scaffold.starter.powerjob;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PowerJob 适配的自动装配类。
 *
 * <p>启用分三步：server 引入本 starter（默认已引入）；配置 {@code scaffold.powerjob.enabled=true}
 * 与 worker 连接参数（powerjob.worker.*，见 application-prod.yml 示例），并把
 * {@code scaffold.job.engine} 改为 none 停掉内置调度；最后在 PowerJob 控制台建任务，
 * 处理器全限定类名填 {@link PowerJobJobAdapter}，参数填 {@code {"jobName":"..."}}。</p>
 *
 * <p>worker 本体的启停由官方 starter 的 {@code powerjob.worker.enabled} 控制（默认 false，
 * 未配连接参数时不会尝试连接调度中心）。</p>
 *
 * @author lianghang
 */
@Configuration
@ConditionalOnProperty(prefix = "scaffold.powerjob", name = "enabled", havingValue = "true")
public class PowerJobAutoConfiguration {

    @Bean
    public PowerJobJobAdapter powerJobJobAdapter(ApplicationContext applicationContext) {
        return new PowerJobJobAdapter(applicationContext);
    }
}
