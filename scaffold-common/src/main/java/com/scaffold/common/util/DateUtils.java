package com.scaffold.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具：全局格式常量与 java.time 常用转换。
 *
 * <p>格式常量的唯一出口——web 层 Jackson 序列化、业务层入库/展示统一引用，
 * 避免「web 一个格式、日志一个格式」的口径漂移。线程安全：{@link DateTimeFormatter} 不可变。</p>
 *
 * @author lianghang
 */
public final class DateUtils {

    /** 全局日期时间格式（web 序列化 / DB 展示统一口径） */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 全局日期格式 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /** 当前时间格式化为全局日期时间串 */
    public static String now() {
        return format(LocalDateTime.now());
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    /** 解析全局格式日期时间串；null/空串返回 null，格式非法抛异常（ fail-fast） */
    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(text.trim(), DATE_TIME_FORMATTER);
    }

    /** 解析全局格式日期串；null/空串返回 null */
    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(text.trim(), DATE_FORMATTER);
    }

    /** LocalDateTime → 系统默认时区毫秒时间戳 */
    public static long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** 毫秒时间戳 → LocalDateTime（系统默认时区） */
    public static LocalDateTime fromEpochMilli(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
    }

    private DateUtils() {
    }
}
