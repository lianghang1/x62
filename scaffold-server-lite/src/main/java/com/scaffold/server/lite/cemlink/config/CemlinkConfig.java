package com.scaffold.server.lite.cemlink.config;

import com.scaffold.server.lite.cemlink.collect.BlockDataCollectionJob;
import com.scaffold.server.lite.cemlink.collect.DepositCollectionJob;
import com.scaffold.server.lite.cemlink.collect.ServiceItemCollectionJob;
import com.scaffold.server.lite.cemlink.collect.TombDataCollectionJob;
import com.scaffold.server.lite.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.server.lite.cemlink.increment.WatermarkStore;
import com.scaffold.server.lite.cemlink.park.DefaultParkStrategy;
import com.scaffold.server.lite.cemlink.park.ParkStrategy;
import com.scaffold.server.lite.cemlink.park.WeijiajiaoParkStrategy;
import com.scaffold.server.lite.cemlink.source.BlockSourceReader;
import com.scaffold.server.lite.cemlink.source.DepositSourceReader;
import com.scaffold.server.lite.cemlink.source.ServiceItemSourceReader;
import com.scaffold.server.lite.cemlink.source.TombSourceReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * cemlink 装配：采集端业务 Bean（cemlink.enabled=true 时生效）
 *
 * <p>数据源分工：墓园库走框架多数据源（{@code scaffold.datasource.dynamic.datasources.park}），
 * Reader 查询由 {@code @TargetDataSource("park")} 切换路由；主库（master）承载
 * 水位线/上行队列/执行日志，JdbcTemplate 由 Boot 自动装配（默认路由主库）</p>
 *
 * <p>园区策略：各园区专属策略类自带 {@code @ConditionalOnProperty(park-code)} 激活条件，
 * 在 {@code @Import} 中登记即可；未命中园区的类型走下方 {@code @ConditionalOnMissingBean} 默认兜底</p>
 *
 * @author lianghang
 */
@Configuration
@Import(WeijiajiaoParkStrategy.class)
@EnableConfigurationProperties(CemlinkProperties.class)
@ConditionalOnProperty(name = "cemlink.enabled", havingValue = "true")
public class CemlinkConfig {

    /** 默认兜底：无单位策略激活时全部走接口 default 通用行为，外部声明同类型 Bean 也可覆盖 */
    @Bean
    @ConditionalOnMissingBean(ParkStrategy.class)
    public ParkStrategy parkStrategy() {
        return new DefaultParkStrategy();
    }

    @Bean
    public BlockSourceReader blockSourceReader(JdbcTemplate jdbcTemplate,
                                               ParkStrategy parkStrategy) {
        return new BlockSourceReader(jdbcTemplate, parkStrategy);
    }

    @Bean
    public TombSourceReader tombSourceReader(JdbcTemplate jdbcTemplate,
                                             ParkStrategy parkStrategy) {
        return new TombSourceReader(jdbcTemplate, parkStrategy);
    }

    @Bean
    public ServiceItemSourceReader serviceItemSourceReader(JdbcTemplate jdbcTemplate) {
        return new ServiceItemSourceReader(jdbcTemplate);
    }

    @Bean
    public DepositSourceReader depositSourceReader(JdbcTemplate jdbcTemplate) {
        return new DepositSourceReader(jdbcTemplate);
    }

    /** linkora 客户端未启用（无 server-url）时不注册采集任务 */
    @Bean
    @ConditionalOnProperty("linkora.client.server-url")
    public TombDataCollectionJob tombDataCollectionJob(TombSourceReader reader,
                                                       CemeteryLinkoraClient cemeteryLinkoraClient,
                                                       WatermarkStore watermarkStore,
                                                       CemlinkProperties properties) {
        return new TombDataCollectionJob(reader, cemeteryLinkoraClient, watermarkStore, properties);
    }

    @Bean
    @ConditionalOnProperty("linkora.client.server-url")
    public BlockDataCollectionJob blockDataCollectionJob(BlockSourceReader reader,
                                                         CemeteryLinkoraClient cemeteryLinkoraClient,
                                                         WatermarkStore watermarkStore) {
        return new BlockDataCollectionJob(reader, cemeteryLinkoraClient, watermarkStore);
    }

    @Bean
    @ConditionalOnProperty("linkora.client.server-url")
    public ServiceItemCollectionJob serviceItemCollectionJob(ServiceItemSourceReader reader,
                                                             CemeteryLinkoraClient cemeteryLinkoraClient,
                                                             WatermarkStore watermarkStore,
                                                             CemlinkProperties properties) {
        return new ServiceItemCollectionJob(reader, cemeteryLinkoraClient, watermarkStore,
                properties.parseInitialWatermark());
    }

    @Bean
    @ConditionalOnProperty("linkora.client.server-url")
    public DepositCollectionJob depositCollectionJob(DepositSourceReader reader,
                                                     CemeteryLinkoraClient cemeteryLinkoraClient,
                                                     WatermarkStore watermarkStore,
                                                     CemlinkProperties properties) {
        return new DepositCollectionJob(reader, cemeteryLinkoraClient, watermarkStore,
                properties.parseInitialWatermark());
    }
}
