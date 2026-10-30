package com.scaffold.common;

import com.scaffold.common.util.AmountUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AmountUtil 测试：null 安全、统一 2 位 HALF_UP、分元转换。
 *
 * @author lianghang
 */
class AmountUtilTest {

    @Test
    void addSubtractNullSafe() {
        assertEquals(new BigDecimal("3.00"), AmountUtil.add(new BigDecimal("1.5"), new BigDecimal("1.50")));
        assertEquals(new BigDecimal("1.50"), AmountUtil.add(null, new BigDecimal("1.5")));
        assertEquals(new BigDecimal("1.00"), AmountUtil.subtract(new BigDecimal("2.5"), new BigDecimal("1.5")));
        assertEquals(new BigDecimal("0.00"), AmountUtil.subtract(null, null));
    }

    @Test
    void multiplyDivideWithScale() {
        // 金额 × 费率：1.5 × 0.333 = 0.49995 → 0.50
        assertEquals(new BigDecimal("0.50"),
                AmountUtil.multiply(new BigDecimal("1.5"), new BigDecimal("0.333")));
        // 10 ÷ 3 = 3.33（HALF_UP）
        assertEquals(new BigDecimal("3.33"), AmountUtil.divide(BigDecimal.TEN, new BigDecimal("3")));
        assertThrows(ArithmeticException.class, () -> AmountUtil.divide(BigDecimal.TEN, BigDecimal.ZERO));
    }

    @Test
    void fenYuanConversion() {
        assertEquals(1500L, AmountUtil.toFen(new BigDecimal("15.00")));
        assertEquals(999L, AmountUtil.toFen(new BigDecimal("9.99")));
        assertEquals(0L, AmountUtil.toFen(null));
        assertEquals(new BigDecimal("9.99"), AmountUtil.fromFen(999));
    }

    @Test
    void positiveCheck() {
        assertTrue(AmountUtil.isPositive(new BigDecimal("0.01")));
        assertFalse(AmountUtil.isPositive(BigDecimal.ZERO));
        assertFalse(AmountUtil.isPositive(null));
        assertFalse(AmountUtil.isPositive(new BigDecimal("-1")));
    }
}
