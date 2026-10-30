package com.scaffold.starter.security.identity;

/**
 * 系统统一账户类型的枚举。
 *
 * <p>三类入口殊途同归——无论 /online 微信授权、/internal 账密登录还是 /openapi 签名认证，
 * 认证通过后都落成 sys_account 中的一种账户；下游鉴权按本类型区分角色与 scope 的加载方式。</p>
 *
 * @author lianghang
 */
public enum AccountType {

    /** C 端用户（/online，微信授权后创建） */
    MEMBER,

    /** 内部操作员（/internal，账密登录，挂 RBAC 角色） */
    OPERATOR,

    /** 三方系统（/openapi，appKey/appSecret 签名，挂 scope） */
    APP
}
