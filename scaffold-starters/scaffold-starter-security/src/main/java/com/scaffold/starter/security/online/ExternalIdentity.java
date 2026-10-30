package com.scaffold.starter.security.online;

/**
 * 外部身份源解析出的用户标识与资料。
 *
 * <p>openId 是三方平台侧的唯一标识，用于关联本地 sys_account；
 * unionId/nickname/avatar 为可选资料，仅供建档与展示——框架不假设外部平台结构。</p>
 *
 * @author lianghang
 */
public class ExternalIdentity {

    private String provider;

    /** 外部平台用户唯一标识（openid） */
    private String openId;

    /** 可选：unionId */
    private String unionId;

    private String nickname;

    private String avatar;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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
}
