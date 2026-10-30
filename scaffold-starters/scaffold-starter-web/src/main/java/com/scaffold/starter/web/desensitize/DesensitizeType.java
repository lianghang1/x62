package com.scaffold.starter.web.desensitize;

/**
 * 脱敏类型枚举，定义各类敏感信息的打码规则。
 *
 * <p>清单与 yudao 的 desensitize 保持同款、够用为准；长度不足或格式异常的值
 * 统一整体打码，避免截取越界泄漏片段。</p>
 *
 * @author lianghang
 */
public enum DesensitizeType {

    /** 手机号：138****1234。 */
    PHONE,

    /** 邮箱：a****@example.com。 */
    EMAIL,

    /** 身份证：110101********1234。 */
    ID_CARD,

    /** 银行卡：6222****5678。 */
    BANK_CARD,

    /** 姓名：张****。 */
    NAME,

    /** 自定义：前 prefixKeep 位 + **** + 后 suffixKeep 位。 */
    CUSTOM
}
