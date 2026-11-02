package com.scaffold.module.cemlink;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * H2 测试函数：模拟 MySQL DATE_FORMAT（仅测试 SQL 覆盖到的格式）
 *
 * @author lianghang
 */
public class H2Functions {

    public static String dateFormat(Timestamp value, String pattern) {
        if (value == null) {
            return null;
        }
        if ("%Y-%m-%d".equals(pattern)) {
            return new SimpleDateFormat("yyyy-MM-dd").format(value);
        }
        throw new IllegalArgumentException("测试仅支持 %Y-%m-%d，实际: " + pattern);
    }
}
