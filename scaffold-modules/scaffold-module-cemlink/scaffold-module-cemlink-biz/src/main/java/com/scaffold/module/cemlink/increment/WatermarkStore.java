package com.scaffold.module.cemlink.increment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 本地水位线（SQLite 独立文件，不写墓园业务库）：按 (域, 业务ID) 记录已推送版本
 *
 * @author lianghang
 */
public class WatermarkStore implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(WatermarkStore.class);

    private final Connection connection;

    public WatermarkStore(Path dbFile) {
        try {
            SQLiteDataSource ds = new SQLiteDataSource();
            ds.setUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
            this.connection = ds.getConnection();
            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS cemlink_watermark ("
                        + "domain TEXT NOT NULL, biz_id TEXT NOT NULL, "
                        + "version INTEGER NOT NULL, updated_at TEXT NOT NULL, "
                        + "PRIMARY KEY (domain, biz_id))");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("水位线库初始化失败: " + dbFile, e);
        }
    }

    /** 版本大于已推送版本（或未推送过）才需要推 */
    public synchronized boolean needPush(String domain, String bizId, int version) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT version FROM cemlink_watermark WHERE domain = ? AND biz_id = ?")) {
            ps.setString(1, domain);
            ps.setString(2, bizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return true;
                }
                return version > rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("水位线查询失败: " + domain + "/" + bizId, e);
        }
    }

    public synchronized void markPushed(String domain, String bizId, int version) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO cemlink_watermark"
                        + " (domain, biz_id, version, updated_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, domain);
            ps.setString(2, bizId);
            ps.setInt(3, version);
            ps.setString(4, java.time.LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("水位线写入失败: " + domain + "/" + bizId, e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("水位线库关闭失败", e);
        }
    }
}
