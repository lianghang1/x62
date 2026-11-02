package com.scaffold.cemlink.client.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 售后订单下行数据（对齐原契约 ServiceOrderData）
 *
 * @author lianghang
 */
public class ServiceOrderData {

    /** server 订单ID */
    private String id;
    /** 订单号 */
    private String orderNo;
    /** 微商订单类型 */
    private String orderType;
    /** 订单状态 */
    private String orderStatus;
    /** 墓穴业务ID */
    private String tombId;
    /** 寄存业务ID */
    private String depositId;
    /** 订单时间 */
    private Date date;
    /** 开始时间 */
    private Date startTime;
    /** 结束时间 */
    private Date endTime;
    /** 付款金额，单位：分 */
    private Long amount;
    /** 数量 */
    private Integer qty;
    /** 付款时间 */
    private Date payTime;
    /** 支付方式 */
    private Integer payType;
    /** 支付流水号 */
    private String paySerialNo;
    /** 发票号 */
    private String invoiceNo;
    /** 电子印章文档ID */
    private String dzyzDocumentId;
    /** 备注 */
    private String remark;
    /** 联系人姓名 */
    private String name;
    /** 联系人手机 */
    private String phone;
    /** 完工时间 */
    private Date completeTime;
    /** 完工人 */
    private String completeUser;
    /** 完工人ID */
    private String completeUserId;
    /** 完工结果 */
    private String completeResult;
    /** 扩展数据 */
    private Map<String, String> extendData;
    /** 对象型扩展数据 */
    private Map<String, Object> extendDataObj;
    /** 其他数据（在线售墓使用人） */
    private List<Map<String, String>> otherData;
    /** 订购服务明细 */
    private List<ServiceOrderReserveData> services;
    /** 发票明细 */
    private List<ServiceOrderInvoiceData> invoices;
    /** 订单图片 */
    private List<OrderImageData> orderImages;
    /** 完工图片 */
    private List<OrderImageData> completeImages;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getTombId() {
        return tombId;
    }

    public void setTombId(String tombId) {
        this.tombId = tombId;
    }

    public String getDepositId() {
        return depositId;
    }

    public void setDepositId(String depositId) {
        this.depositId = depositId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public Integer getPayType() {
        return payType;
    }

    public void setPayType(Integer payType) {
        this.payType = payType;
    }

    public String getPaySerialNo() {
        return paySerialNo;
    }

    public void setPaySerialNo(String paySerialNo) {
        this.paySerialNo = paySerialNo;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getDzyzDocumentId() {
        return dzyzDocumentId;
    }

    public void setDzyzDocumentId(String dzyzDocumentId) {
        this.dzyzDocumentId = dzyzDocumentId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
    }

    public String getCompleteUser() {
        return completeUser;
    }

    public void setCompleteUser(String completeUser) {
        this.completeUser = completeUser;
    }

    public String getCompleteUserId() {
        return completeUserId;
    }

    public void setCompleteUserId(String completeUserId) {
        this.completeUserId = completeUserId;
    }

    public String getCompleteResult() {
        return completeResult;
    }

    public void setCompleteResult(String completeResult) {
        this.completeResult = completeResult;
    }

    public Map<String, String> getExtendData() {
        return extendData;
    }

    public void setExtendData(Map<String, String> extendData) {
        this.extendData = extendData;
    }

    public Map<String, Object> getExtendDataObj() {
        return extendDataObj;
    }

    public void setExtendDataObj(Map<String, Object> extendDataObj) {
        this.extendDataObj = extendDataObj;
    }

    public List<Map<String, String>> getOtherData() {
        return otherData;
    }

    public void setOtherData(List<Map<String, String>> otherData) {
        this.otherData = otherData;
    }

    public List<ServiceOrderReserveData> getServices() {
        return services;
    }

    public void setServices(List<ServiceOrderReserveData> services) {
        this.services = services;
    }

    public List<ServiceOrderInvoiceData> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<ServiceOrderInvoiceData> invoices) {
        this.invoices = invoices;
    }

    public List<OrderImageData> getOrderImages() {
        return orderImages;
    }

    public void setOrderImages(List<OrderImageData> orderImages) {
        this.orderImages = orderImages;
    }

    public List<OrderImageData> getCompleteImages() {
        return completeImages;
    }

    public void setCompleteImages(List<OrderImageData> completeImages) {
        this.completeImages = completeImages;
    }
}
