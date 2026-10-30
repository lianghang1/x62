package com.scaffold.starter.storage;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储门面，业务只依赖本接口。
 *
 * <p>内部委托 x-file-storage，业务不感知其与具体平台 SDK；
 * 平台切换只改配置（dromara.x-file-storage.*），代码零改动——存储实现与业务彻底解耦。</p>
 *
 * @author lianghang
 */
public class ScaffoldStorageService {

    private final FileStorageService fileStorageService;

    public ScaffoldStorageService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /** 上传到默认平台 */
    public FileInfo upload(MultipartFile file, String bizPath) {
        return fileStorageService.of(file)
                .setPath(bizPath + "/")
                .upload();
    }

    /** 上传到指定平台（dromara.x-file-storage 配置中的平台名） */
    public FileInfo upload(MultipartFile file, String bizPath, String platform) {
        return fileStorageService.of(file)
                .setPlatform(platform)
                .setPath(bizPath + "/")
                .upload();
    }

    public void delete(FileInfo fileInfo) {
        fileStorageService.delete(fileInfo);
    }
}
