package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * MEMBER 扩展信息（微信侧资料）。字段增长不影响主表。
 */
@Entity
@Table(name = "member_info")
public class MemberInfo extends BaseEntity {

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "open_id", length = 128)
    private String openId;

    @Column(name = "union_id", length = 128)
    private String unionId;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "avatar", length = 512)
    private String avatar;

    @Column(name = "phone", length = 20)
    private String phone;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
