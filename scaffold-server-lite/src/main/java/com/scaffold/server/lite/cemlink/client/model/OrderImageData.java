package com.scaffold.server.lite.cemlink.client.model;

/**
 * 订单图片（对齐原契约 OrderImageData）
 *
 * @author lianghang
 */
public class OrderImageData {

    /** 图片地址 */
    private String url;
    /** 缩略图片地址 */
    private String thumbUrl;
    /** 图片类型 */
    private String imageType;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }
}
