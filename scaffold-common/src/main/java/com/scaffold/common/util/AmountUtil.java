package com.scaffold.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额计算工具：金额一律用 {@link BigDecimal}，禁止 double/float 参与运算。
 *
 * <p>统一规约：金额存储用「元」的 DECIMAL(12,2)，运算结果统一 2 位小数 + 四舍五入
 * （{@code HALF_UP}，银行家舍入对用户不直观）；分↔元转换用于对接支付渠道接口。</p>
 *
 * @author lianghang
 */
public final class AmountUtil {

    /** 金额统一小数位 */
    public static final int SCALE = 2;

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** 加法；null 按 0 处理，返回 2 位小数 */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return nvl(a).add(nvl(b)).setScale(SCALE, ROUNDING);
    }

    /** 减法；null 按 0 处理，返回 2 位小数 */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return nvl(a).subtract(nvl(b)).setScale(SCALE, ROUNDING);
    }

    /** 乘法（金额×数量/费率）；null 按 0 处理，返回 2 位小数 */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return nvl(a).multiply(nvl(b)).setScale(SCALE, ROUNDING);
    }

    /** 除法（金额÷n）；除数非法抛异常；返回 2 位小数 */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b == null || b.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("金额除法除数不能为 0 或 null");
        }
        return nvl(a).divide(b, SCALE, ROUNDING);
    }

    /** 元 → 分（渠道接口对接）；返回 0 表示入参为 null 或 0 元 */
    public static long toFen(BigDecimal yuan) {
        return yuan == null ? 0L : yuan.movePointRight(2).setScale(0, ROUNDING).longValueExact();
    }

    /** 分 → 元 */
    public static BigDecimal fromFen(long fen) {
        return BigDecimal.valueOf(fen).movePointLeft(2).setScale(SCALE, ROUNDING);
    }

    /**
     * 是否为正金额（&gt; 0），下单金额校验常用。
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private AmountUtil() {
    }
}
