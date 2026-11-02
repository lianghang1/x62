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

    /** 初始水位时间（yyyy-MM-dd HH:mm:ss）：该时间之前的存量数据不采集，空则首轮全量 */
    private String initialWatermark;

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

    public String getInitialWatermark() {
        return initialWatermark;
    }

    public void setInitialWatermark(String initialWatermark) {
        this.initialWatermark = initialWatermark;
    }

    /** 解析初始水位时间为 Date，未配置或格式非法返回 null */
    public java.util.Date parseInitialWatermark() {
        if (initialWatermark == null || initialWatermark.trim().isEmpty()) {
            return null;
        }
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(initialWatermark.trim());
        } catch (java.text.ParseException e) {
            throw new IllegalStateException("cemlink.initial-watermark 格式应为 yyyy-MM-dd HH:mm:ss: " + initialWatermark, e);
        }
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
