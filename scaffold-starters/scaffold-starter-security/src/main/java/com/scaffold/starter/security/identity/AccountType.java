package com.scaffold.starter.security.identity;

/**
 * 统一账户类型：三类入口殊途同归——认证通过后都落成 sys_account 中的一种账户。
 */
public enum AccountType {

    /** C 端用户（/online，微信授权后创建） */
    MEMBER,

    /** 内部操作员（/internal，账密登录，挂 RBAC 角色） */
    OPERATOR,

    /** 三方系统（/openapi，appKey/appSecret 签名，挂 scope） */
    APP
}
