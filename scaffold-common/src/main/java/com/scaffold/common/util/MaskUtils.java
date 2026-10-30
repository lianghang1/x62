package com.scaffold.common.util;

/**
 * 敏感信息打码工具。
 *
 * <p>starter-web 的 {@code @Desensitize} 注解序列化时委托本类，非 JSON 场景
 * （日志打印、导出、短信文案）也直接调用同名方法，保证全工程打码规则只有一份。
 * 掩码固定为四星 {@code ****}；源串过短不足以保留前后缀时整体打码。</p>
 *
 * @author lianghang
 */
public final class MaskUtils {

    /** 统一掩码字符 */
    public static final String MASK = "****";

    private MaskUtils() {
    }

    /** 手机号：保留前 3 后 4，如 {@code 138****5678}。 */
    public static String phone(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= 7) {
            return MASK;
        }
        return value.substring(0, 3) + MASK + value.substring(value.length() - 4);
    }

    /** 邮箱：保留首字符与域名，如 {@code a****@example.com}。 */
    public static String email(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int at = value.indexOf('@');
        if (at <= 0) {
            return MASK;
        }
        return value.charAt(0) + MASK + value.substring(at);
    }

    /** 身份证号：保留前 6 后 4，如 {@code 110101********1234}。 */
    public static String idCard(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= 10) {
            return MASK;
        }
        return value.substring(0, 6) + MASK + value.substring(value.length() - 4);
    }

    /** 银行卡号：保留前 4 后 4，如 {@code 6222****1234}。 */
    public static String bankCard(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= 8) {
            return MASK;
        }
        return value.substring(0, 4) + MASK + value.substring(value.length() - 4);
    }

    /** 姓名：保留姓氏（单字姓名整体打码），如 {@code 张****}。 */
    public static String name(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.length() == 1 ? MASK : value.charAt(0) + MASK;
    }

    /**
     * 自定义打码：保留前 prefixKeep 位、后 suffixKeep 位，中间打码。
     *
     * <p>前后缀合计超过源串长度时按能保留的最大长度截取，不会越界。</p>
     */
    public static String custom(String value, int prefixKeep, int suffixKeep) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int prefix = Math.min(prefixKeep, value.length());
        int suffix = value.length() - prefix >= suffixKeep ? suffixKeep : 0;
        return value.substring(0, prefix) + MASK
                + (suffix > 0 ? value.substring(value.length() - suffix) : "");
    }
}
