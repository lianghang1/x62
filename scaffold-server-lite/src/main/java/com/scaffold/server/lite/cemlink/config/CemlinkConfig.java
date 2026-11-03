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
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Paths;

import javax.sql.DataSource;

/**
 * cemlink 装配：墓园本地库只读数据源
 *
 * <p>园区策略：各园区专属策略类自带 {@code @ConditionalOnProperty(park-code)} 激活条件，
 * 在 {@code @Import} 中登记即可；未命中园区的类型走下方 {@code @ConditionalOnMissingBean} 默认兜底</p>
 *
 * @author lianghang
 */
@Configuration
@Import(WeijiajiaoParkStrategy.class)
@EnableConfigurationProperties(CemlinkProperties.class)
@ConditionalOnProperty("cemlink.source.url")
public class CemlinkConfig {

    /** 墓园库采集源：上行采集只读，工单/服务项/寄存下行回写需要写权限 */
    @Bean(name = "cemlinkParkDataSource")
    public DataSource cemlinkParkDataSource(CemlinkProperties properties) {
        CemlinkProperties.Source source = properties.getSource();
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(source.getUrl());
        ds.setUsername(source.getUsername());
        ds.setPassword(source.getPassword());
        ds.setDriverClassName(source.getDriverClassName());
        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(2);
        ds.setPoolName("cemlink-park");
        return ds;
    }

    @Bean(name = "cemlinkParkJdbc")
    public JdbcTemplate cemlinkParkJdbc(DataSource cemlinkParkDataSource) {
        return new JdbcTemplate(cemlinkParkDataSource);
    }

    /** 默认兜底：无单位策略激活时全部走接口 default 通用行为，外部声明同类型 Bean 也可覆盖 */
    @Bean
    @ConditionalOnMissingBean(ParkStrategy.class)
    public ParkStrategy parkStrategy() {
        return new DefaultParkStrategy();
    }

    @Bean
    public BlockSourceReader blockSourceReader(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc,
            ParkStrategy parkStrategy) {
        return new BlockSourceReader(cemlinkParkJdbc, parkStrategy);
    }

    @Bean
    public TombSourceReader tombSourceReader(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc,
            ParkStrategy parkStrategy) {
        return new TombSourceReader(cemlinkParkJdbc, parkStrategy);
    }

    @Bean
    public ServiceItemSourceReader serviceItemSourceReader(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc) {
        return new ServiceItemSourceReader(cemlinkParkJdbc);
    }

    @Bean
    public DepositSourceReader depositSourceReader(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc) {
        return new DepositSourceReader(cemlinkParkJdbc);
    }

    @Bean(destroyMethod = "close")
    public WatermarkStore watermarkStore(CemlinkProperties properties) {
        return new WatermarkStore(Paths.get(properties.getWatermarkDbPath()));
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
