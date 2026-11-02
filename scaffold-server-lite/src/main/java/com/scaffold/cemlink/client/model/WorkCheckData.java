package com.scaffold.cemlink.client.model;

import java.util.Date;

/**
 * 工单验收下行数据（对齐 server 契约 WorkCheckData）
 *
 * @author lianghang
 */
public class WorkCheckData {

    /** server 工单验收记录ID */
    private String id;
    /** 工单ID（墓园库 t_sfi_work.id） */
    private String workId;
    /** 验收状态 */
    private String checkStatus;
    /** 验收时间 */
    private Date checkTime;
    /** 电子印章文档ID */
    private String dzyzDocumentId;
    /** 电子印章显示地址 */
    private String dzyzDocumentPath;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkId() {
        return workId;
    }

    public void setWorkId(String workId) {
        this.workId = workId;
    }

    public String getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(String checkStatus) {
        this.checkStatus = checkStatus;
    }

    public Date getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(Date checkTime) {
        this.checkTime = checkTime;
    }

    public String getDzyzDocumentId() {
        return dzyzDocumentId;
    }

    public void setDzyzDocumentId(String dzyzDocumentId) {
        this.dzyzDocumentId = dzyzDocumentId;
    }

    public String getDzyzDocumentPath() {
        return dzyzDocumentPath;
    }

    public void setDzyzDocumentPath(String dzyzDocumentPath) {
        this.dzyzDocumentPath = dzyzDocumentPath;
    }
}
