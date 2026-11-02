package com.scaffold.cemlink;

import com.scaffold.cemlink.client.MallLinkoraClient;
import com.scaffold.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.cemlink.client.model.DepositBindData;
import com.scaffold.cemlink.client.model.DepositData;
import com.scaffold.cemlink.client.model.WorkCheckData;
import com.scaffold.cemlink.collect.DepositCollectionJob;
import com.scaffold.cemlink.down.DepositBindDownlinkHandler;
import com.scaffold.cemlink.down.WorkDownlinkHandler;
import com.scaffold.cemlink.increment.WatermarkStore;
import com.scaffold.cemlink.source.DepositSourceReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 寄存上行采集 + 工单验收/寄存绑定下行回写：H2 模拟墓园库直测
 *
 * @author lianghang
 */
public class DepositWorkDownlinkTest {

    private JdbcTemplate parkJdbc;
    private RecordingMallLinkoraClient recordingMallClient;
    private WatermarkStore watermark;
    private DepositCollectionJob depositJob;
    private WorkDownlinkHandler workHandler;
    private DepositBindDownlinkHandler bindHandler;

    /** 录制上行发布：topic + 原始 payload 对象 */
    static class RecordingMallLinkoraClient extends MallLinkoraClient {

        final List<String> topics = new CopyOnWriteArrayList<>();
        final List<Object> payloads = new CopyOnWriteArrayList<>();

        RecordingMallLinkoraClient() {
            super(null);
        }

        @Override
        public String publish(String topic, Object payload, String sequenceKey) {
            topics.add(topic);
            payloads.add(payload);
            return "fake-" + topics.size();
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        parkJdbc = new JdbcTemplate(h2DataSource());
        createSchema();
        seedData();

        recordingMallClient = new RecordingMallLinkoraClient();
        CemeteryLinkoraClient cemeteryClient = new CemeteryLinkoraClient(recordingMallClient);
        workHandler = new WorkDownlinkHandler(parkJdbc, recordingMallClient);
        bindHandler = new DepositBindDownlinkHandler(parkJdbc, recordingMallClient);

        Path watermarkDb = Files.createTempDirectory("cemlink-wm").resolve("watermark.db");
        watermark = new WatermarkStore(watermarkDb);
        depositJob = new DepositCollectionJob(new DepositSourceReader(parkJdbc), cemeteryClient, watermark, null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (watermark != null) {
            watermark.close();
        }
    }

    @Test
    public void depositUplinkMapsStatusAndFees() {
        depositJob.run();

        assertEquals(1, recordingMallClient.topics.size());
        assertEquals("cemetery-tomb-ashes-deposit", recordingMallClient.topics.get(0));
        @SuppressWarnings("unchecked")
        List<DepositData> payload = (List<DepositData>) recordingMallClient.payloads.get(0);
        assertEquals(2, payload.size(), "无效业务不推送");

        // 格位单价路径：gridFee 10.00 × boxSize 2 × 100 = 2000 分
        DepositData first = findByBizNo(payload, "BZ001");
        assertEquals("1", first.getState(), "存入应映射为已存");
        assertEquals("张三", first.getDirectorName(), "承办人姓名应去空格");
        assertEquals("y", first.getDepositCharge());
        assertEquals(Long.valueOf(2000L), first.getPrice());
        assertNull(first.getManagePrice());
        assertTrue(first.getRenewable());

        // 管理费路径：(depositFee 90 + 管理费30) / 3年 × 100 = 4000 分，管理费 3000 分
        DepositData second = findByBizNo(payload, "BZ002");
        assertEquals("3", second.getState(), "登记应映射为预订");
        assertEquals(Long.valueOf(4000L), second.getPrice());
        assertEquals(Long.valueOf(3000L), second.getManagePrice());
    }

    @Test
    public void depositUplinkWatermarkSkipsUnchanged() {
        depositJob.run();
        depositJob.run();
        assertEquals(1, recordingMallClient.topics.size(), "无修改不应重复推送");

        parkJdbc.update("UPDATE t_sfi_ashes_business SET modify_date = {ts '2027-06-01 00:00:00'} "
                + "WHERE id = 'B1'");
        depositJob.run();
        assertEquals(2, recordingMallClient.topics.size(), "修改时间推进应再次推送");
    }

    @Test
    public void workDownlinkChecksAndPublishesUpdates() {
        parkJdbc.update("INSERT INTO t_sfi_work (id, work_status, is_validate) VALUES (55, '3', '1')");
        parkJdbc.update("INSERT INTO t_sfi_work (id, work_status, is_validate) VALUES (56, '3', '1')");

        WorkCheckData first = new WorkCheckData();
        first.setId("wc-1");
        first.setWorkId("55");
        WorkCheckData invalidWorkId = new WorkCheckData();
        invalidWorkId.setId("wc-2");
        invalidWorkId.setWorkId("abc");
        WorkCheckData missingWork = new WorkCheckData();
        missingWork.setId("wc-3");
        missingWork.setWorkId("999");

        workHandler.handle(Arrays.asList(first, invalidWorkId, missingWork));

        // 55 已验收，56 未在下行批次中保持原状态，非法/不存在工单跳过
        assertEquals("4", parkJdbc.queryForObject(
                "SELECT work_status FROM t_sfi_work WHERE id = 55", String.class));
        assertEquals("3", parkJdbc.queryForObject(
                "SELECT work_status FROM t_sfi_work WHERE id = 56", String.class));
        Long passCount = parkJdbc.queryForObject(
                "SELECT COUNT(1) FROM t_sfi_work_pass WHERE work_id = 55", Long.class);
        assertEquals(Long.valueOf(1L), passCount);
        assertEquals("1", parkJdbc.queryForObject(
                "SELECT check_status FROM t_sfi_work_pass WHERE work_id = 55", String.class));
        assertEquals(Integer.valueOf(9999), parkJdbc.queryForObject(
                "SELECT operate_user_id FROM t_sfi_work_pass WHERE work_id = 55", Integer.class));

        assertEquals(1, recordingMallClient.topics.size());
        assertEquals("cemetery-tomb-work-update", recordingMallClient.topics.get(0));
        @SuppressWarnings("unchecked")
        List<String> updates = (List<String>) recordingMallClient.payloads.get(0);
        assertEquals(Collections.singletonList("wc-1"), updates);
    }

    @Test
    public void depositBindDownlinkSkipsDuplicateCertificate() {
        parkJdbc.update("INSERT INTO t_sfi_ashes_business (id, business_no, is_validate, pay_status) "
                + "VALUES ('B9', 'BZ009', '1', '1')");

        DepositBindData bind = new DepositBindData();
        bind.setId("bd-1");
        bind.setDepositId("B9");
        bind.setUserName("王五");
        bind.setUserIdNo("310104199001011234");
        bind.setIdUserName("王五证件名");
        bind.setIdUserPhone("13800000000");

        bindHandler.handle(Collections.singletonList(bind));

        Long count = parkJdbc.queryForObject(
                "SELECT COUNT(1) FROM t_sfi_ashes_bus_family_member WHERE business_id = 'B9'", Long.class);
        assertEquals(Long.valueOf(1L), count);
        assertEquals("王五", parkJdbc.queryForObject(
                "SELECT name FROM t_sfi_ashes_bus_family_member WHERE business_id = 'B9'", String.class));
        assertEquals("（来自网络【公众号】绑定格位）", parkJdbc.queryForObject(
                "SELECT remark FROM t_sfi_ashes_bus_family_member WHERE business_id = 'B9'", String.class));

        // 重复绑定同证件号不重复插入，但同样回传
        bindHandler.handle(Collections.singletonList(bind));
        count = parkJdbc.queryForObject(
                "SELECT COUNT(1) FROM t_sfi_ashes_bus_family_member WHERE business_id = 'B9'", Long.class);
        assertEquals(Long.valueOf(1L), count);

        assertEquals(2, recordingMallClient.topics.size());
        assertEquals("cemetery-tomb-ashes-deposit-bind-update", recordingMallClient.topics.get(1));
        @SuppressWarnings("unchecked")
        List<String> updates = (List<String>) recordingMallClient.payloads.get(1);
        assertEquals(Collections.singletonList("bd-1"), updates);
    }

    private DepositData findByBizNo(List<DepositData> payload, String bizNo) {
        for (DepositData data : payload) {
            if (bizNo.equals(data.getBizNo())) {
                return data;
            }
        }
        return null;
    }

    private static DriverManagerDataSource h2DataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:h2:mem:park_deposit_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }

    private void createSchema() {
        // DATE_FORMAT 为原 SQL 依赖的 MySQL 函数，H2 经别名模拟
        parkJdbc.execute("CREATE ALIAS IF NOT EXISTS DATE_FORMAT FOR \""
                + H2Functions.class.getName() + ".dateFormat\"");
        // 工单域
        parkJdbc.execute("CREATE TABLE t_sfi_work (id BIGINT PRIMARY KEY, work_status VARCHAR(4), "
                + "modify_date TIMESTAMP, modify_user INT, is_validate VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_work_pass (id BIGINT PRIMARY KEY, work_id BIGINT, "
                + "operate_date TIMESTAMP, operate_user_id INT, operate_user VARCHAR(64), "
                + "operate_version INT, check_status VARCHAR(4), create_date TIMESTAMP, "
                + "create_user INT, is_validate VARCHAR(2))");
        // 寄存域上行
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_business (id VARCHAR(32) PRIMARY KEY, "
                + "business_no VARCHAR(32), grid_paths VARCHAR(256), status VARCHAR(4), "
                + "user_names VARCHAR(128), is_validate VARCHAR(2), pay_status VARCHAR(4), "
                + "create_date TIMESTAMP, modify_date TIMESTAMP)");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_linkman (id VARCHAR(32) PRIMARY KEY, "
                + "business_id VARCHAR(32), is_validate VARCHAR(2), name VARCHAR(64), "
                + "mobile_no VARCHAR(20))");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_user (id VARCHAR(32) PRIMARY KEY, "
                + "business_id VARCHAR(32), qr_code VARCHAR(128), box_size VARCHAR(8))");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_deposit (id VARCHAR(32) PRIMARY KEY, "
                + "business_id VARCHAR(32), last_flag VARCHAR(2), is_validate VARCHAR(2), "
                + "deposit_year INT, deposit_fee DOUBLE, start_date TIMESTAMP, end_date TIMESTAMP)");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_grid (id VARCHAR(32) PRIMARY KEY, "
                + "business_id VARCHAR(32), is_validate VARCHAR(2), is_transfer_flag VARCHAR(2), "
                + "grid_id VARCHAR(32))");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_grid (id VARCHAR(32) PRIMARY KEY, "
                + "deposit_fee DOUBLE, grid_area VARCHAR(32), layer VARCHAR(16), block_id VARCHAR(32))");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_block (id VARCHAR(32) PRIMARY KEY, "
                + "is_on_line VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_item (id BIGINT PRIMARY KEY, "
                + "business_id VARCHAR(32), bill_type VARCHAR(8), is_validate VARCHAR(2), "
                + "`sum` DOUBLE)");
        // 寄存域下行绑定
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_family_member (id BIGINT PRIMARY KEY, "
                + "business_id VARCHAR(32), name VARCHAR(64), certificate_no VARCHAR(32), "
                + "mobile_no VARCHAR(20), remark VARCHAR(128), create_date TIMESTAMP, "
                + "create_user INT, is_validate VARCHAR(2))");
    }

    @SuppressWarnings("unused")
    private void seedData() {
        Date now = new Date();
        // B1：存入 + 格位单价路径，B2：登记 + 管理费路径，B3：无效业务不推送
        parkJdbc.update("INSERT INTO t_sfi_ashes_business (id, business_no, status, user_names, "
                + "is_validate, pay_status, create_date) VALUES ('B1', 'BZ001', '2', '张三', '1', '1', {ts '2024-01-01 00:00:00'})");
        parkJdbc.update("INSERT INTO t_sfi_ashes_business (id, business_no, status, user_names, "
                + "is_validate, pay_status, create_date) VALUES ('B2', 'BZ002', '1', '李四', '1', '2', {ts '2024-02-01 00:00:00'})");
        parkJdbc.update("INSERT INTO t_sfi_ashes_business (id, business_no, status, user_names, "
                + "is_validate, pay_status, create_date) VALUES ('B3', 'BZ003', '2', '王五', '0', '1', {ts '2024-03-01 00:00:00'})");

        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_linkman (id, business_id, is_validate, name, mobile_no) "
                + "VALUES ('L1', 'B1', '1', ' 张 三 ', '13800000000')");
        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_linkman (id, business_id, is_validate, name, mobile_no) "
                + "VALUES ('L2', 'B2', '1', '李四', '13900000000')");

        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_user (id, business_id, qr_code, box_size) "
                + "VALUES ('U1', 'B1', 'QR001', '2')");
        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_user (id, business_id, qr_code, box_size) "
                + "VALUES ('U2', 'B2', 'QR002', '1')");

        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_deposit (id, business_id, last_flag, is_validate, "
                + "deposit_year, deposit_fee, start_date, end_date) VALUES "
                + "('D1', 'B1', '1', '1', 3, 90.0, {ts '2024-01-01 00:00:00'}, {ts '2027-01-01 00:00:00'})");
        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_deposit (id, business_id, last_flag, is_validate, "
                + "deposit_year, deposit_fee, start_date, end_date) VALUES "
                + "('D2', 'B2', '1', '1', 3, 90.0, {ts '2024-02-01 00:00:00'}, {ts '2027-02-01 00:00:00'})");

        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_grid (id, business_id, is_validate, "
                + "is_transfer_flag, grid_id) VALUES ('G1', 'B1', '1', '0', 'GR1')");
        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_grid (id, business_id, is_validate, "
                + "is_transfer_flag, grid_id) VALUES ('G2', 'B2', '1', '0', 'GR2')");

        parkJdbc.update("INSERT INTO t_sfi_ashes_grid (id, deposit_fee, grid_area, layer, block_id) "
                + "VALUES ('GR1', 10.0, '0.5', '2', 'BLK1')");
        // GR2 无格位单价 → B2 走管理费计算路径
        parkJdbc.update("INSERT INTO t_sfi_ashes_grid (id, deposit_fee, grid_area, layer, block_id) "
                + "VALUES ('GR2', NULL, '0.5', '2', 'BLK2')");

        parkJdbc.update("INSERT INTO t_sfi_ashes_block (id, is_on_line) VALUES ('BLK1', '1')");
        parkJdbc.update("INSERT INTO t_sfi_ashes_block (id, is_on_line) VALUES ('BLK2', '1')");

        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_item (id, business_id, bill_type, is_validate, `sum`) "
                + "VALUES (1, 'B2', '0002', '1', 30.0)");
    }
}
