package com.scaffold.module.cemlink.config;

import com.linkora.client.LinkoraClient;
import com.linkora.client.UplinkPublisher;
import com.scaffold.module.cemlink.client.MallLinkoraClient;
import com.scaffold.module.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.module.cemlink.down.CemlinkDownlinkRegistrar;
import com.scaffold.module.cemlink.down.DepositBindDownlinkHandler;
import com.scaffold.module.cemlink.down.OrderDownlinkHandler;
import com.scaffold.module.cemlink.down.ServiceItemDownlinkHandler;
import com.scaffold.module.cemlink.down.WorkDownlinkHandler;
import com.scaffold.starter.http.ScaffoldHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Bean
    @ConditionalOnBean(name = "cemlinkParkJdbc")
    public ServiceItemDownlinkHandler serviceItemDownlinkHandler(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc,
            MallLinkoraClient mallLinkoraClient) {
        return new ServiceItemDownlinkHandler(cemlinkParkJdbc, mallLinkoraClient);
    }

    @Bean
    @ConditionalOnBean(name = "cemlinkParkJdbc")
    public WorkDownlinkHandler workDownlinkHandler(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc,
            MallLinkoraClient mallLinkoraClient) {
        return new WorkDownlinkHandler(cemlinkParkJdbc, mallLinkoraClient);
    }

    @Bean
    @ConditionalOnBean(name = "cemlinkParkJdbc")
    public DepositBindDownlinkHandler depositBindDownlinkHandler(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc,
            MallLinkoraClient mallLinkoraClient) {
        return new DepositBindDownlinkHandler(cemlinkParkJdbc, mallLinkoraClient);
    }

    @Bean
    @ConditionalOnBean(name = "cemlinkParkJdbc")
    public OrderDownlinkHandler orderDownlinkHandler(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc,
            MallLinkoraClient mallLinkoraClient,
            ObjectProvider<ScaffoldHttpClient> scaffoldHttpClient,
            CemlinkProperties properties) {
        return new OrderDownlinkHandler(cemlinkParkJdbc, mallLinkoraClient,
                scaffoldHttpClient.getIfAvailable(), properties.getUploadPath());
    }

    @Bean
    @ConditionalOnBean({LinkoraClient.class, ServiceItemDownlinkHandler.class,
            WorkDownlinkHandler.class, DepositBindDownlinkHandler.class})
    public CemlinkDownlinkRegistrar cemlinkDownlinkRegistrar(LinkoraClient linkoraClient,
                                                             ServiceItemDownlinkHandler serviceItemDownlinkHandler,
                                                             WorkDownlinkHandler workDownlinkHandler,
                                                             DepositBindDownlinkHandler depositBindDownlinkHandler,
                                                             OrderDownlinkHandler orderDownlinkHandler) {
        return new CemlinkDownlinkRegistrar(linkoraClient,
                serviceItemDownlinkHandler, workDownlinkHandler, depositBindDownlinkHandler, orderDownlinkHandler);
    }
}
