package com.scaffold.common;

import com.scaffold.common.util.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DateUtils 测试：全局格式常量与转换。
 *
 * @author lianghang
 */
class DateUtilsTest {

    @Test
    void formatAndParseRoundtrip() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 30, 22, 45, 0);
        String text = DateUtils.format(time);
        assertEquals("2026-10-30 22:45:00", text);
        assertEquals(time, DateUtils.parseDateTime(text));
    }

    @Test
    void nullAndEmptySafe() {
        assertNull(DateUtils.format(null));
        assertNull(DateUtils.parseDateTime(null));
        assertNull(DateUtils.parseDateTime("  "));
        assertNull(DateUtils.formatDate(null));
        assertNull(DateUtils.parseDate(null));
    }

    @Test
    void epochMilliRoundtrip() {
        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        long milli = DateUtils.toEpochMilli(time);
        assertEquals(time, DateUtils.fromEpochMilli(milli));
    }

    @Test
    void datePatternConstants() {
        assertEquals("yyyy-MM-dd HH:mm:ss", DateUtils.DATE_TIME_PATTERN);
        assertEquals("yyyy-MM-dd", DateUtils.DATE_PATTERN);
        assertEquals("2026-10-30", DateUtils.formatDate(LocalDate.of(2026, 10, 30)));
    }

    @Test
    void javaUtilDateRoundtrip() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 30, 8, 30, 0);
        java.util.Date date = DateUtils.toDate(time);
        assertEquals(time, DateUtils.toLocalDateTime(date));
        assertNull(DateUtils.toDate(null));
        assertNull(DateUtils.toLocalDateTime(null));
    }

    @Test
    void dayBalanceSecondsInDay() {
        long seconds = DateUtils.dayBalanceSeconds();
        assertTrue(seconds > 0 && seconds <= 86400);
    }

    @Test
    void compactFormat() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 30, 22, 45, 9);
        assertEquals("20261030224509", DateUtils.formatCompact(time));
        assertNull(DateUtils.formatCompact(null));
    }

    @Test
    void chineseWeek() {
        // 2026-10-30 是星期五
        assertEquals("星期五", DateUtils.chineseWeek(LocalDate.of(2026, 10, 30)));
        assertNull(DateUtils.chineseWeek(null));
    }
}
