package com.scaffold.module.cemlink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkora.client.LinkoraClient;
import com.linkora.client.UplinkPublisher;
import com.linkora.client.storage.sqlite.SqliteClientStore;
import com.linkora.core.message.OutboxStatus;
import com.linkora.testkit.EmbeddedLinkoraServer;
import com.scaffold.module.cemlink.client.MallLinkoraClient;
import com.scaffold.module.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.module.cemlink.collect.TombDataCollectionJob;
import com.scaffold.module.cemlink.config.CemlinkProperties;
import com.scaffold.module.cemlink.increment.WatermarkStore;
import com.scaffold.module.cemlink.park.WeijiajiaoTombParkStrategy;
import com.scaffold.module.cemlink.source.TombSourceReader;
import com.sfi.mall.contract.base.model.cemetery.TransportTomb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 墓穴采集端到端：H2 模拟墓园库 → 采集 Job → linkora 内嵌服务端收发
 *
 * @author lianghang
 */
public class TombEndToEndTest {

    private static final String CLIENT_ID = "cemlink-test";

    private EmbeddedLinkoraServer testServer;
    private SqliteClientStore clientStore;
    private LinkoraClient linkoraClient;
    private WatermarkStore watermark;
    private JdbcTemplate parkJdbc;
    private RecordingCemeteryClient cemeteryClient;
    private TombDataCollectionJob job;

    /** 录制上传批次与 messageId，供断言与等待服务端确认 */
    static class RecordingCemeteryClient extends CemeteryLinkoraClient {

        final List<List<TransportTomb>> batches = new CopyOnWriteArrayList<>();
        final List<String> messageIds = new CopyOnWriteArrayList<>();

        RecordingCemeteryClient(MallLinkoraClient mallLinkoraClient) {
            super(mallLinkoraClient);
        }

        @Override
        public String uploadTombs(List<TransportTomb> data) {
            batches.add(new ArrayList<>(data));
            String id = super.uploadTombs(data);
            messageIds.add(id);
            return id;
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        parkJdbc = new JdbcTemplate(h2DataSource());
        createSchema();
        seedData();

        testServer = EmbeddedLinkoraServer.builder()
                .registerClient(CLIENT_ID)
                .build();
        testServer.start();

        Path clientDb = Files.createTempDirectory("cemlink-client").resolve("client.db");
        clientStore = new SqliteClientStore(clientDb);
        linkoraClient = LinkoraClient.builder()
                .serverUrl(testServer.baseUrl())
                .clientId(CLIENT_ID)
                .clientStore(clientStore)
                .build();
        linkoraClient.start();

        MallLinkoraClient mallClient = new MallLinkoraClient(new UplinkPublisher(linkoraClient, clientStore));
        cemeteryClient = new RecordingCemeteryClient(mallClient);

        Path watermarkDb = Files.createTempDirectory("cemlink-wm").resolve("watermark.db");
        watermark = new WatermarkStore(watermarkDb);

        TombSourceReader reader =
                new TombSourceReader(parkJdbc, new WeijiajiaoTombParkStrategy());
        CemlinkProperties properties = new CemlinkProperties();
        job = new TombDataCollectionJob(reader, cemeteryClient, watermark, properties);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (linkoraClient != null) {
            linkoraClient.close();
        }
        if (clientStore != null) {
            clientStore.close();
        }
        if (testServer != null) {
            testServer.close();
        }
        if (watermark != null) {
            watermark.close();
        }
    }

    @Test
    public void fullPipelineCollectsAssemblesAndDelivers() throws Exception {
        job.run();

        assertEquals(1, cemeteryClient.batches.size(), "首轮应推送一个批次（两条）");
        List<TransportTomb> batch = cemeteryClient.batches.get(0);
        assertEquals(2, batch.size());

        TransportTomb first = findByTombId(batch, "T001");
        assertNotNull(first);
        assertEquals("A-01", first.getTombNo());
        assertEquals("->A区->01", first.getTombPath());
        assertEquals("1", first.getState());
        assertEquals("2", first.getPayState());
        assertEquals(Boolean.TRUE, first.getRentingFlag());
        assertEquals(Long.valueOf(7200L), first.getAmountPerYear());
        assertNotNull(first.getMaintenanceEndTime());
        assertNotNull(first.getBuyTime());
        assertEquals("标准墓", first.getTypeCode());
        assertEquals("入土", first.getBuryStyle());
        assertEquals("张三", first.getDirectorName());
        assertEquals("13800000000", first.getDirectorPhone());

        // 退墓业务（tomb_id 为空、走 return_tomb_id）→ 状态2 且无购墓ID
        TransportTomb second = null;
        for (TransportTomb tomb : batch) {
            if ("李四".equals(tomb.getDirectorName())) {
                second = tomb;
            }
        }
        assertNotNull(second);
        assertEquals("2", second.getState(), "无购墓ID应记状态2");
        assertEquals(null, second.getTombId());

        awaitServerAccepted(0);

        // 上行 payload JSON 结构断言（MallLinkoraData 信封 + 字段名与契约一致）
        String raw = new String(
                clientStore.findOutbox(cemeteryClient.messageIds.get(0)).get().getPayload(),
                java.nio.charset.StandardCharsets.UTF_8);
        JsonNode root = new ObjectMapper().readTree(raw);
        assertTrue(root.has("payload"));
        assertEquals("T001", root.path("payload").get(0).path("tombId").asText());
        assertEquals("A-01", root.path("payload").get(0).path("tombNo").asText());
        assertEquals(7200L, root.path("payload").get(0).path("amountPerYear").asLong());
    }

    @Test
    public void watermarkSkipsUnchangedRowsOnSecondRun() {
        job.run();
        assertEquals(1, cemeteryClient.batches.size());

        job.run();
        assertEquals(1, cemeteryClient.batches.size(), "无版本变化不应重复推送");
    }

    @Test
    public void versionBumpRepublishesOnlyChangedRow() throws Exception {
        job.run();
        assertEquals(1, cemeteryClient.batches.size());

        parkJdbc.update("UPDATE t_sfi_business SET collect_version = 3 WHERE id = '1001'");
        job.run();

        assertEquals(2, cemeteryClient.batches.size(), "版本升级的行应再次推送");
        List<TransportTomb> secondBatch = cemeteryClient.batches.get(1);
        assertEquals(1, secondBatch.size());
        assertEquals("T001", secondBatch.get(0).getTombId());
        awaitServerAccepted(1);
    }

    @Test
    public void oldCollectionRecordsKeepFilteringCollectedRows() {
        // 1003 在墓园库已有采集记录（collection_version = collect_version = 2），SQL 条件应排除
        job.run();
        assertEquals(1, cemeteryClient.batches.size());
        assertEquals(2, cemeteryClient.batches.get(0).size());
    }

    private void awaitServerAccepted(int index) throws InterruptedException {
        String messageId = cemeteryClient.messageIds.get(index);
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (clientStore.findOutbox(messageId)
                    .map(entry -> entry.getStatus() == OutboxStatus.SERVER_ACCEPTED)
                    .orElse(false)) {
                return;
            }
            Thread.sleep(50);
        }
        assertTrue(false, "消息 " + messageId + " 未在超时内被服务端接受");
    }

    private TransportTomb findByTombId(List<TransportTomb> batch, String tombId) {
        for (TransportTomb tomb : batch) {
            if (tombId.equals(tomb.getTombId())) {
                return tomb;
            }
        }
        return null;
    }

    private static DriverManagerDataSource h2DataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:h2:mem:park_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }

    private void createSchema() {
        parkJdbc.execute("CREATE ALIAS IF NOT EXISTS DATE_FORMAT FOR \""
                + H2Functions.class.getName() + ".dateFormat\"");
        parkJdbc.execute("CREATE TABLE t_sfi_business (id VARCHAR(32) PRIMARY KEY, "
                + "is_validate VARCHAR(2), pay_status VARCHAR(4), tomb_id VARCHAR(32), "
                + "return_tomb_id VARCHAR(32), collect_version INT)");
        parkJdbc.execute("CREATE TABLE t_sfi_bus_linkman (id VARCHAR(32) PRIMARY KEY, "
                + "business_id VARCHAR(32), is_validate VARCHAR(2), name VARCHAR(64), "
                + "mobile_no VARCHAR(20))");
        parkJdbc.execute("CREATE TABLE t_sfi_bus_detail (id VARCHAR(32) PRIMARY KEY, "
                + "business_id VARCHAR(32), is_validate VARCHAR(2), apply_date TIMESTAMP)");
        parkJdbc.execute("CREATE TABLE t_sfi_tomb (id VARCHAR(32) PRIMARY KEY, "
                + "layer_code VARCHAR(64), tomb_path VARCHAR(128), block_id VARCHAR(32), "
                + "grave_no INT, tomb_style VARCHAR(32), bury_style_id VARCHAR(32), "
                + "fact_area DECIMAL(10,2), sell_area DECIMAL(10,2), basic_price DECIMAL(12,2), "
                + "maintenance_year INT, maintenance_fee DOUBLE)");
        parkJdbc.execute("CREATE TABLE t_sfi_tomb_style (id VARCHAR(32) PRIMARY KEY, "
                + "name VARCHAR(64))");
        parkJdbc.execute("CREATE TABLE t_sfi_code_bury_style (id VARCHAR(32) PRIMARY KEY, "
                + "name VARCHAR(64), style_type_id VARCHAR(32))");
        parkJdbc.execute("CREATE TABLE t_sfi_code_bury_style_type (id VARCHAR(32) PRIMARY KEY, "
                + "outdoor_flag VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_bus_collection_detail (bus_id VARCHAR(32), "
                + "collection_version INT)");
        parkJdbc.execute("CREATE TABLE t_sfi_bus_maintenance (id VARCHAR(32) PRIMARY KEY, "
                + "business_id VARCHAR(32), is_validate VARCHAR(2), maint_year INT, "
                + "maint_fee DOUBLE, maint_index INT, end_date TIMESTAMP)");
    }

    private void seedData() {
        parkJdbc.update("INSERT INTO t_sfi_tomb_style (id, name) VALUES ('S1', '标准墓')");
        parkJdbc.update("INSERT INTO t_sfi_code_bury_style (id, name, style_type_id) "
                + "VALUES ('B1', '入土', 'ST1')");
        parkJdbc.update("INSERT INTO t_sfi_code_bury_style_type (id, outdoor_flag) "
                + "VALUES ('ST1', '1')");
        parkJdbc.update("INSERT INTO t_sfi_tomb (id, layer_code, tomb_path, block_id, grave_no, "
                + "tomb_style, bury_style_id, fact_area, sell_area, basic_price, "
                + "maintenance_year, maintenance_fee) VALUES "
                + "('T001', '卫家角A-01', '卫家角->A区->01', 'BLK1', 1, 'S1', 'B1', 1.50, 3.00, 12800.00, 5, 360.0)");
        parkJdbc.update("INSERT INTO t_sfi_tomb (id, layer_code, tomb_path, block_id, grave_no, "
                + "tomb_style, bury_style_id, fact_area, sell_area, basic_price, "
                + "maintenance_year, maintenance_fee) VALUES "
                + "('T002', '卫家角B-02', '卫家角->B区->02', 'BLK2', 2, 'S1', 'B1', 2.00, 4.00, 15800.00, 10, 600.0)");

        parkJdbc.update("INSERT INTO t_sfi_business (id, is_validate, pay_status, tomb_id, "
                + "return_tomb_id, collect_version) VALUES ('1001', '1', '3', 'T001', NULL, 2)");
        parkJdbc.update("INSERT INTO t_sfi_business (id, is_validate, pay_status, tomb_id, "
                + "return_tomb_id, collect_version) VALUES ('1002', '1', '3', NULL, 'T002', 1)");
        parkJdbc.update("INSERT INTO t_sfi_business (id, is_validate, pay_status, tomb_id, "
                + "return_tomb_id, collect_version) VALUES ('1003', '1', '3', 'T001', NULL, 2)");

        parkJdbc.update("INSERT INTO t_sfi_bus_linkman (id, business_id, is_validate, name, mobile_no) "
                + "VALUES ('L1', '1001', '1', '张三', '13800000000')");
        parkJdbc.update("INSERT INTO t_sfi_bus_linkman (id, business_id, is_validate, name, mobile_no) "
                + "VALUES ('L2', '1002', '1', '李四', '13900000000')");
        parkJdbc.update("INSERT INTO t_sfi_bus_linkman (id, business_id, is_validate, name, mobile_no) "
                + "VALUES ('L3', '1003', '1', '王五', '13700000000')");

        parkJdbc.update("INSERT INTO t_sfi_bus_detail (id, business_id, is_validate, apply_date) "
                + "VALUES ('D1', '1001', '1', {ts '2024-01-15 00:00:00'})");
        parkJdbc.update("INSERT INTO t_sfi_bus_detail (id, business_id, is_validate, apply_date) "
                + "VALUES ('D2', '1002', '1', {ts '2024-02-20 00:00:00'})");
        parkJdbc.update("INSERT INTO t_sfi_bus_detail (id, business_id, is_validate, apply_date) "
                + "VALUES ('D3', '1003', '1', {ts '2024-03-10 00:00:00'})");

        parkJdbc.update("INSERT INTO t_sfi_bus_maintenance (id, business_id, is_validate, "
                + "maint_year, maint_fee, maint_index, end_date) VALUES "
                + "('M1', '1001', '1', 5, 360.0, 1, {ts '2029-01-15 00:00:00'})");

        parkJdbc.update("INSERT INTO t_sfi_bus_collection_detail (bus_id, collection_version) "
                + "VALUES ('1003', 2)");
    }
}
