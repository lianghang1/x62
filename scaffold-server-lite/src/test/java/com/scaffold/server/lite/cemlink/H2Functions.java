package com.scaffold.server.lite.cemlink;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * H2 测试函数：模拟 MySQL DATE_FORMAT 与 getTombBlockChildList（仅测试 SQL 覆盖到的行为）
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

    public static String tombBlockChildList(Connection conn, String blockId) throws Exception {
        StringBuilder sb = new StringBuilder();
        appendChildren(conn, blockId, sb);
        return sb.toString();
    }

    private static void appendChildren(Connection conn, String parentId, StringBuilder sb) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM t_sfi_tomb_block WHERE super_id = ? AND is_validate = '1'")) {
            ps.setString(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    sb.append(',').append(id);
                    appendChildren(conn, id, sb);
                }
            }
        }
    }
}

