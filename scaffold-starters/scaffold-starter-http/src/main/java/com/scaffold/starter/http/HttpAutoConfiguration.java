package com.scaffold.starter.http;

import com.scaffold.starter.http.sign.RequestSigner;
import com.scaffold.starter.http.transport.HttpTransport;
import com.scaffold.starter.http.transport.OkHttpTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * HTTP 客户端自动装配入口。
 *
 * <p>transport 默认 okhttp；业务注册 {@link RequestSigner}
 * Bean 即全局生效签名；自定义 {@link HttpTransport} Bean 可整体替换实现。</p>
 *
 * @author lianghang
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "scaffold.http", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(HttpProperties.class)
public class HttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(HttpTransport.class)
    public HttpTransport httpTransport(HttpProperties properties) {
        return new OkHttpTransport(properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    @Bean
    @ConditionalOnMissingBean
    public ScaffoldHttpClient scaffoldHttpClient(HttpTransport transport,
                                                 HttpProperties properties,
                                                 ObjectProvider<RequestSigner> signerProvider) {
        return new ScaffoldHttpClient(transport, properties, signerProvider.getIfAvailable());
    }
}
