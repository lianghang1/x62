package com.scaffold.server.lite.cemlink.config;

import com.scaffold.common.util.DateUtils;
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

    /** 初始水位时间（yyyy-MM-dd HH:mm:ss）：该时间之前的存量数据不采集，空则首轮全量 */
    private String initialWatermark;

    /** 订单图片下载根目录 */
    private String uploadPath = "./uploadFile";

    private final Collect collect = new Collect();

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

    public String getInitialWatermark() {
        return initialWatermark;
    }

    public void setInitialWatermark(String initialWatermark) {
        this.initialWatermark = initialWatermark;
    }

    /** 解析初始水位时间为 Date，未配置返回 null，格式非法 fail-fast */
    public java.util.Date parseInitialWatermark() {
        java.time.LocalDateTime dateTime = DateUtils.parseDateTime(initialWatermark);
        return dateTime == null ? null
                : java.util.Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public Collect getCollect() {
        return collect;
    }
}
