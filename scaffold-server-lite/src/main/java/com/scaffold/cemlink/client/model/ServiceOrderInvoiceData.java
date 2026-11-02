package com.scaffold.cemlink.client.model;

import java.util.Date;

/**
 * 订单发票明细（对齐原契约 ServiceOrderInvoiceData）
 *
 * @author lianghang
 */
public class ServiceOrderInvoiceData {

    /** 发票类型，0：蓝票；1：红票 */
    private String invoiceType;
    /** 发票代码 */
    private String invoiceCode;
    /** 发票号码 */
    private String invoiceNo;
    /** 发票抬头 */
    private String invoiceTitle;
    /** 开票人 */
    private String invoiceUser;
    /** 合计金额，单位：分 */
    private Long totalAmount;
    /** 合计税额，单位：分 */
    private Long totalTax;
    /** 流水号 */
    private String serialNo;
    /** 开票时间 */
    private Date invoiceTime;
    /** PDF 地址 */
    private String pdfUrl;
    /** H5 地址 */
    private String h5Url;
    /** 图片地址 */
    private String imgUrl;

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getInvoiceTitle() {
        return invoiceTitle;
    }

    public void setInvoiceTitle(String invoiceTitle) {
        this.invoiceTitle = invoiceTitle;
    }

    public String getInvoiceUser() {
        return invoiceUser;
    }

    public void setInvoiceUser(String invoiceUser) {
        this.invoiceUser = invoiceUser;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(Long totalTax) {
        this.totalTax = totalTax;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public Date getInvoiceTime() {
        return invoiceTime;
    }

    public void setInvoiceTime(Date invoiceTime) {
        this.invoiceTime = invoiceTime;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getH5Url() {
        return h5Url;
    }

    public void setH5Url(String h5Url) {
        this.h5Url = h5Url;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
