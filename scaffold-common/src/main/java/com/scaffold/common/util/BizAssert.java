package com.scaffold.common.util;

import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.ErrorCode;
import com.scaffold.common.exception.GlobalErrorCode;

/**
 * 业务断言工具：校验失败直接抛 {@link BizException}。
 *
 * <p>与 hutool {@code Assert} 的区别：抛的是业务异常而非 IllegalArgumentException，
 * 全局异常处理器会转成带错误码的统一响应。守卫语句写在方法开头，
 * 校验与失败语义一行表达，替代散落的 if/throw 模板。</p>
 *
 * @author lianghang
 */
public final class BizAssert {

    private BizAssert() {
    }

    /** 条件不成立时抛出指定错误码的业务异常。 */
    public static void isTrue(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new BizException(errorCode);
        }
    }

    /** 条件不成立时抛出指定错误码 + 自定义文案的业务异常。 */
    public static void isTrue(boolean expression, ErrorCode errorCode, String message) {
        if (!expression) {
            throw new BizException(errorCode, message);
        }
    }

    /** 对象为 null 时抛出业务异常（默认 1000 请求参数错误）。 */
    public static <T> T notNull(T object, ErrorCode errorCode) {
        if (object == null) {
            throw new BizException(errorCode);
        }
        return object;
    }

    /** 对象为 null 时抛出业务异常 + 自定义文案。 */
    public static <T> T notNull(T object, ErrorCode errorCode, String message) {
        if (object == null) {
            throw new BizException(errorCode, message);
        }
        return object;
    }

    /** 字符串空白（null/空串/纯空白）时抛出业务异常 + 自定义文案。 */
    public static String notBlank(String text, ErrorCode errorCode, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw new BizException(errorCode, message);
        }
        return text;
    }

    /** 无条件抛出业务异常（用于 switch 兜底、不可达分支等）。 */
    public static void fail(ErrorCode errorCode) {
        throw new BizException(errorCode);
    }

    /** 无条件抛出业务异常 + 自定义文案。 */
    public static void fail(ErrorCode errorCode, String message) {
        throw new BizException(errorCode, message);
    }

    /** 参数类断言的默认错误码快捷方法：条件不成立按 1000 请求参数错误抛出。 */
    public static void param(boolean expression, String message) {
        if (!expression) {
            throw new BizException(GlobalErrorCode.BAD_REQUEST, message);
        }
    }
}
