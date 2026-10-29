package com.scaffold.starter.storage.config;

import com.scaffold.starter.storage.ScaffoldStorageService;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储自动装配（依赖 x-file-storage 自身的 EnableFileStorage/自动装配完成平台注册）。
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
