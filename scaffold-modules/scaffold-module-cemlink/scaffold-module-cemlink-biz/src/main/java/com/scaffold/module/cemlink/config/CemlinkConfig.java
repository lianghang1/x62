package com.scaffold.module.cemlink.config;

import com.scaffold.module.cemlink.collect.BlockDataCollectionJob;
import com.scaffold.module.cemlink.collect.TombDataCollectionJob;
import com.scaffold.module.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.module.cemlink.increment.WatermarkStore;
import com.scaffold.module.cemlink.park.BlockParkStrategy;
import com.scaffold.module.cemlink.park.DefaultBlockParkStrategy;
import com.scaffold.module.cemlink.park.DefaultTombParkStrategy;
import com.scaffold.module.cemlink.park.TombParkStrategy;
import com.scaffold.module.cemlink.park.WeijiajiaoBlockParkStrategy;
import com.scaffold.module.cemlink.park.WeijiajiaoTombParkStrategy;
import com.scaffold.module.cemlink.source.BlockSourceReader;
import com.scaffold.module.cemlink.source.TombSourceReader;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Paths;

import javax.sql.DataSource;

/**
 * cemlink 装配：墓园本地库只读数据源
 *
 * @author lianghang
 */
@Configuration
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

    /** 园区差异覆盖点：特殊园区声明同类型 Bean 即替换默认实现 */
    @Bean
    @ConditionalOnProperty(name = "cemlink.park-code", havingValue = "weijiaojiao")
    public TombParkStrategy weijiaojiaoTombParkStrategy() {
        return new WeijiajiaoTombParkStrategy();
    }

    @Bean
    @ConditionalOnProperty(name = "cemlink.park-code", havingValue = "weijiaojiao")
    public BlockParkStrategy weijiaojiaoBlockParkStrategy() {
        return new WeijiajiaoBlockParkStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(TombParkStrategy.class)
    public TombParkStrategy tombParkStrategy() {
        return new DefaultTombParkStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(BlockParkStrategy.class)
    public BlockParkStrategy blockParkStrategy() {
        return new DefaultBlockParkStrategy();
    }

    @Bean
    public BlockSourceReader blockSourceReader(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc,
            BlockParkStrategy blockParkStrategy) {
        return new BlockSourceReader(cemlinkParkJdbc, blockParkStrategy);
    }

    @Bean
    public TombSourceReader tombSourceReader(
            @Qualifier("cemlinkParkJdbc") JdbcTemplate cemlinkParkJdbc,
            TombParkStrategy parkStrategy) {
        return new TombSourceReader(cemlinkParkJdbc, parkStrategy);
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
}
