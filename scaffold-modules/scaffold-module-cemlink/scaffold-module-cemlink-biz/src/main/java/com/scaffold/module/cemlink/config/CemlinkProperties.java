package com.scaffold.module.cemlink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * cemlink 配置属性
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "cemlink")
public class CemlinkProperties {

    /** 园区编码（多园区差异化配置的路由键） */
    private String parkCode;

    /** 本地水位线 SQLite 文件路径 */
    private String watermarkDbPath = "./linkora/cemlink-watermark.db";

    /** 订单图片下载根目录 */
    private String uploadPath = "./uploadFile";

    private final Source source = new Source();

    private final Collect collect = new Collect();

    /** 墓园本地库只读数据源 */
    public static class Source {

        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }

    /** 采集行为 */
    public static class Collect {

        /** 每页条数（与旧程序一致） */
        private int pageSize = 200;

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }

    public String getParkCode() {
        return parkCode;
    }

    public void setParkCode(String parkCode) {
        this.parkCode = parkCode;
    }

    public String getWatermarkDbPath() {
        return watermarkDbPath;
    }

    public void setWatermarkDbPath(String watermarkDbPath) {
        this.watermarkDbPath = watermarkDbPath;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public Source getSource() {
        return source;
    }

    public Collect getCollect() {
        return collect;
    }
}
