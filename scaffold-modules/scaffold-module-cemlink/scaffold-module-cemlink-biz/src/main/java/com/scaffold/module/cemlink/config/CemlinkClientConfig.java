package com.scaffold.module.cemlink.config;

import com.linkora.client.UplinkPublisher;
import com.scaffold.module.cemlink.client.MallLinkoraClient;
import com.scaffold.module.cemlink.client.cemetery.CemeteryLinkoraClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * linkora 客户端封装装配
 *
 * @author lianghang
 */
@Configuration
public class CemlinkClientConfig {

    @Bean
    @ConditionalOnBean(UplinkPublisher.class)
    public MallLinkoraClient mallLinkoraClient(UplinkPublisher publisher) {
        return new MallLinkoraClient(publisher);
    }

    @Bean
    @ConditionalOnBean(MallLinkoraClient.class)
    public CemeteryLinkoraClient cemeteryLinkoraClient(MallLinkoraClient mallLinkoraClient) {
        return new CemeteryLinkoraClient(mallLinkoraClient);
    }
}
