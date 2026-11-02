package com.scaffold.cemlink.client.model;

import java.util.Date;

/**
 * 寄存绑定下行数据（对齐 server 契约 DepositBindData）
 *
 * @author lianghang
 */
public class DepositBindData {

    /** server 绑定记录ID */
    private String id;
    /** 寄存业务ID（墓园库 t_sfi_ashes_business.id） */
    private String depositId;
    /** 用户姓名 */
    private String userName;
    /** 用户身份证 */
    private String userIdNo;
    /** 绑定时间 */
    private Date bindTime;
    /** 用户名称 */
    private String idUserName;
    /** 用户手机号 */
    private String idUserPhone;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDepositId() {
        return depositId;
    }

    public void setDepositId(String depositId) {
        this.depositId = depositId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserIdNo() {
        return userIdNo;
    }

    public void setUserIdNo(String userIdNo) {
        this.userIdNo = userIdNo;
    }

    public Date getBindTime() {
        return bindTime;
    }

    public void setBindTime(Date bindTime) {
        this.bindTime = bindTime;
    }

    public String getIdUserName() {
        return idUserName;
    }

    public void setIdUserName(String idUserName) {
        this.idUserName = idUserName;
    }

    public String getIdUserPhone() {
        return idUserPhone;
    }

    public void setIdUserPhone(String idUserPhone) {
        this.idUserPhone = idUserPhone;
    }
}
