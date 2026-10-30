package com.scaffold.starter.storage.config;

import com.scaffold.starter.storage.ScaffoldStorageService;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储的自动装配类。
 *
 * <p>平台注册由 x-file-storage 自身的 EnableFileStorage 与自动装配完成，
 * 本类仅按需装配 ScaffoldStorageService 门面——各司其职，不重复造轮子。</p>
 *
 * @author lianghang
 */
@Configuration
@ConditionalOnClass(FileStorageService.class)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ScaffoldStorageService scaffoldStorageService(FileStorageService fileStorageService) {
        return new ScaffoldStorageService(fileStorageService);
    }
}
