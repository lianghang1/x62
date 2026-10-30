package com.scaffold.module.system.biz.entity;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 会员扩展信息表。
 *
 * <p>存放 MEMBER 账户的微信侧资料，与主表 {@code SysAccount} 一对一分离：
 * 扩展字段的增减不影响账户主表结构，各自独立演进。</p>
 *
 * @author lianghang
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
