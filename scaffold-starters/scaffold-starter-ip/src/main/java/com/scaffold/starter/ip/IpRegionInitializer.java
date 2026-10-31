package com.scaffold.starter.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * IP 归属地库启动加载器：在环境准备阶段（刷新上下文之前）读取
 * {@code scaffold.ip.enabled} 开关，开启时把 classpath 下的
 * ip2region.xdb 一次性加载进内存，首个请求即可零成本查询。
 *
 * <p>经 META-INF/spring.factories 注册——{@code ApplicationEnvironmentPreparedEvent}
 * 早于任何 Bean 创建，Bean 方式的监听器收不到该事件。</p>
 *
 * @author lianghang
 */
public class IpRegionInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(IpRegionInitializer.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        if ("false".equalsIgnoreCase(env.getProperty("scaffold.ip.enabled"))) {
            log.info("IP 归属地库已通过 scaffold.ip.enabled=false 关闭，跳过加载");
            return;
        }
        IpRegionUtil.init();
    }
}
