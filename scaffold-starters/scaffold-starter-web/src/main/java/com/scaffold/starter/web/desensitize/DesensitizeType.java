package com.scaffold.starter.web.desensitize;

/**
 * 脱敏类型（yudao desensitize 同款清单，够用为准）。
 */
public enum DesensitizeType {

    /** 手机号：138****1234 */
    PHONE,

    /** 邮箱：a***@example.com */
    EMAIL,

    /** 身份证：110101********1234 */
    ID_CARD,

    /** 银行卡：6222****5678 */
    BANK_CARD,

    /** 姓名：张** */
    NAME,

    /** 自定义：前 prefixKeep 位 + **** + 后 suffixKeep 位 */
    CUSTOM
}
