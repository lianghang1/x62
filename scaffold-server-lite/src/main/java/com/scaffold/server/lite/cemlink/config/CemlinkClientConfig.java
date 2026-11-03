package com.scaffold.server.lite.cemlink.config;

import com.linkora.client.LinkoraClient;
import com.linkora.client.UplinkPublisher;
import com.scaffold.server.lite.cemlink.client.MallLinkoraClient;
import com.scaffold.server.lite.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.server.lite.cemlink.down.CemlinkDownlinkRegistrar;
import com.scaffold.server.lite.cemlink.down.DepositBindDownlinkHandler;
import com.scaffold.server.lite.cemlink.down.OrderDownlinkHandler;
import com.scaffold.server.lite.cemlink.down.ServiceItemDownlinkHandler;
import com.scaffold.server.lite.cemlink.down.WorkDownlinkHandler;
import com.scaffold.starter.http.ScaffoldHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * linkora 客户端封装装配：linkora 启用（{@code linkora.client.enabled≠false}，
 * 与 LinkoraClientAutoConfiguration 生效条件一致）且墓园库已配置时装配
 *
 * <p>条件一律用 {@code @ConditionalOnProperty}（只看配置文件，与 Bean 注册
 * 时机无关），不使用 {@code @ConditionalOnBean}</p>
 *
 * @author lianghang
 */
@Configuration
@ConditionalOnProperty(prefix = "linkora.client", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class CemlinkClientConfig {

    @Bean
    public MallLinkoraClient mallLinkoraClient(UplinkPublisher publisher) {
        return new MallLinkoraClient(publisher);
    }

    @Bean
    public CemeteryLinkoraClient cemeteryLinkoraClient(MallLinkoraClient mallLinkoraClient) {
        return new CemeteryLinkoraClient(mallLinkoraClient);
    }

    @Bean
    @ConditionalOnProperty(name = "cemlink.enabled", havingValue = "true")
    public ServiceItemDownlinkHandler serviceItemDownlinkHandler(
            JdbcTemplate jdbcTemplate,
            MallLinkoraClient mallLinkoraClient) {
        return new ServiceItemDownlinkHandler(jdbcTemplate, mallLinkoraClient);
    }

    @Bean
    @ConditionalOnProperty(name = "cemlink.enabled", havingValue = "true")
    public WorkDownlinkHandler workDownlinkHandler(
            JdbcTemplate jdbcTemplate,
            MallLinkoraClient mallLinkoraClient) {
        return new WorkDownlinkHandler(jdbcTemplate, mallLinkoraClient);
    }

    @Bean
    @ConditionalOnProperty(name = "cemlink.enabled", havingValue = "true")
    public DepositBindDownlinkHandler depositBindDownlinkHandler(
            JdbcTemplate jdbcTemplate,
            MallLinkoraClient mallLinkoraClient) {
        return new DepositBindDownlinkHandler(jdbcTemplate, mallLinkoraClient);
    }

    @Bean
    @ConditionalOnProperty(name = "cemlink.enabled", havingValue = "true")
    public OrderDownlinkHandler orderDownlinkHandler(
            JdbcTemplate jdbcTemplate,
            MallLinkoraClient mallLinkoraClient,
            ObjectProvider<ScaffoldHttpClient> scaffoldHttpClient,
            CemlinkProperties properties) {
        return new OrderDownlinkHandler(jdbcTemplate, mallLinkoraClient,
                scaffoldHttpClient.getIfAvailable(), properties.getUploadPath());
    }

    @Bean
    @ConditionalOnProperty(name = "cemlink.enabled", havingValue = "true")
    public CemlinkDownlinkRegistrar cemlinkDownlinkRegistrar(LinkoraClient linkoraClient,
                                                             ServiceItemDownlinkHandler serviceItemDownlinkHandler,
                                                             WorkDownlinkHandler workDownlinkHandler,
                                                             DepositBindDownlinkHandler depositBindDownlinkHandler,
                                                             OrderDownlinkHandler orderDownlinkHandler) {
        return new CemlinkDownlinkRegistrar(linkoraClient,
                serviceItemDownlinkHandler, workDownlinkHandler, depositBindDownlinkHandler, orderDownlinkHandler);
    }
}
