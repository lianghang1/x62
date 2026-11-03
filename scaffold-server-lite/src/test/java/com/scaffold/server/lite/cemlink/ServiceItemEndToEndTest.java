package com.scaffold.server.lite.cemlink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkora.client.LinkoraClient;
import com.linkora.client.UplinkPublisher;
import com.linkora.client.storage.sqlite.SqliteClientStore;
import com.linkora.core.message.OutboxStatus;
import com.linkora.testkit.EmbeddedLinkoraServer;
import com.scaffold.server.lite.cemlink.increment.WatermarkStore;
import com.scaffold.server.lite.cemlink.client.MallLinkoraClient;
import com.scaffold.server.lite.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.server.lite.cemlink.client.model.ServiceItemData;
import com.scaffold.server.lite.cemlink.client.model.TombServiceItemData;
import com.scaffold.server.lite.cemlink.collect.ServiceItemCollectionJob;
import com.scaffold.server.lite.cemlink.down.ServiceItemDownlinkHandler;
import com.scaffold.server.lite.cemlink.source.ServiceItemSourceReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务项目端到端：H2 模拟墓园库 → 上行采集 → linkora 收发 → 下行编码回写
 *
 * @author lianghang
 */
public class ServiceItemEndToEndTest {

    private static final String CLIENT_ID = "cemlink-service-test";

    private EmbeddedLinkoraServer testServer;
    private SqliteClientStore clientStore;
    private LinkoraClient linkoraClient;
    private WatermarkStore watermark;
    private JdbcTemplate parkJdbc;
    private RecordingMallLinkoraClient recordingMallClient;
    private RecordingCemeteryClient recordingCemeteryClient;
    private ServiceItemDownlinkHandler downlinkHandler;
    private ServiceItemCollectionJob job;

    /** 录制上行发布：topic + 原始 payload 对象（下行 handler 回传用，不走真实链路） */
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

    /** 录制上行批次与 messageId，供断言与等待服务端确认 */
    static class RecordingCemeteryClient extends CemeteryLinkoraClient {

        final List<List<TombServiceItemData>> batches = new CopyOnWriteArrayList<>();
        final List<String> messageIds = new CopyOnWriteArrayList<>();

        RecordingCemeteryClient(MallLinkoraClient mallLinkoraClient) {
            super(mallLinkoraClient);
        }

        @Override
        public String uploadServiceItems(List<TombServiceItemData> data) {
            batches.add(new ArrayList<>(data));
            String id = super.uploadServiceItems(data);
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

        recordingMallClient = new RecordingMallLinkoraClient();
        recordingCemeteryClient = new RecordingCemeteryClient(
                new MallLinkoraClient(new UplinkPublisher(linkoraClient, clientStore)));
        downlinkHandler = new ServiceItemDownlinkHandler(parkJdbc, recordingMallClient);

        watermark = TestWatermarkStores.inMemory();

        job = new ServiceItemCollectionJob(new ServiceItemSourceReader(parkJdbc), recordingCemeteryClient, watermark, null);
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
    }

    @Test
    public void uplinkCollectsAndFiltersByOrderType() {
        job.run();

        assertEquals(1, recordingCemeteryClient.batches.size());
        List<TombServiceItemData> payload = recordingCemeteryClient.batches.get(0);
        assertEquals(2, payload.size(), "无订单类型的行应被过滤");

        TombServiceItemData first = findById(payload, "101");
        assertEquals("代客祭扫服务", first.getName());
        assertEquals(Long.valueOf(8800L), first.getPrice());
        assertEquals(Integer.valueOf(1), first.getQty());
        assertEquals("6", first.getOrderType(), "代客祭扫子类型应映射订单类型6");
        assertEquals("1", first.getStatus());

        TombServiceItemData second = findById(payload, "102");
        assertEquals("7", second.getOrderType(), "盆花租摆子类型应映射订单类型7");
    }

    @Test
    public void uplinkWatermarkSkipsUnchangedRows() {
        job.run();
        job.run();
        assertEquals(1, recordingCemeteryClient.batches.size(), "无修改不应重复推送");

        parkJdbc.update("UPDATE t_sfi_code_service SET modify_date = {ts '2027-06-01 00:00:00'} WHERE id = 101");
        job.run();
        assertEquals(2, recordingCemeteryClient.batches.size(), "修改时间推进应再次推送");
    }

    @Test
    public void initialWatermarkSkipsHistoryRows() {
        ServiceItemCollectionJob floorJob = new ServiceItemCollectionJob(new ServiceItemSourceReader(parkJdbc),
                recordingCemeteryClient, watermark, java.sql.Timestamp.valueOf("2024-01-01 12:00:00"));
        floorJob.run();

        assertEquals(1, recordingCemeteryClient.batches.size());
        List<TombServiceItemData> payload = recordingCemeteryClient.batches.get(0);
        assertEquals(1, payload.size(), "初始水位之前的 101 应被忽略");
        assertEquals("102", payload.get(0).getId());
    }

    @Test
    public void uplinkDeliveredThroughLinkora() throws Exception {
        job.run();

        String raw = new String(
                clientStore.findOutbox(recordingCemeteryClient.messageIds.get(0)).get().getPayload(),
                java.nio.charset.StandardCharsets.UTF_8);
        JsonNode root = new ObjectMapper().readTree(raw);
        assertTrue(root.has("payload"));
        assertEquals("101", root.path("payload").get(0).path("id").asText());
        assertEquals("代客祭扫服务", root.path("payload").get(0).path("name").asText());
        assertEquals(8800L, root.path("payload").get(0).path("price").asLong());
        assertEquals("6", root.path("payload").get(0).path("orderType").asText());

        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (clientStore.findOutbox(recordingCemeteryClient.messageIds.get(0))
                    .map(entry -> entry.getStatus() == OutboxStatus.SERVER_ACCEPTED)
                    .orElse(false)) {
                return;
            }
            Thread.sleep(50);
        }
        assertTrue(false, "上行消息未在超时内被服务端接受");
    }

    @Test
    public void downlinkWritesServiceAndPublishesCodes() {
        ServiceItemData item = new ServiceItemData();
        item.setId("srv-1");
        item.setName("网上代客祭扫");
        item.setPrice(19900L);
        item.setStatus("1");

        downlinkHandler.handle(Arrays.asList(item));

        // O2O 服务类型自动创建
        Long typeId = parkJdbc.queryForObject(
                "SELECT id FROM t_sfi_code_service_type WHERE name = 'O2O'", Long.class);
        List<Long> serviceIds = parkJdbc.queryForList(
                "SELECT id FROM t_sfi_code_service WHERE service_type_id = ?", Long.class, typeId);
        assertEquals(1, serviceIds.size());

        // 价格分转元（HALF_UP 两位小数）
        String price = parkJdbc.queryForObject(
                "SELECT price FROM t_sfi_code_service WHERE id = ?", String.class, serviceIds.get(0));
        assertEquals(new BigDecimal("199.00"), new BigDecimal(price));

        // 回传编码
        assertEquals(1, recordingMallClient.topics.size());
        assertEquals("cemetery-tomb-service-item-update", recordingMallClient.topics.get(0));
        @SuppressWarnings("unchecked")
        List<TombServiceItemData> updates =
                (List<TombServiceItemData>) recordingMallClient.payloads.get(0);
        assertEquals("srv-1", updates.get(0).getId());
        assertEquals(String.valueOf(serviceIds.get(0)), updates.get(0).getCode());
    }

    private TombServiceItemData findById(List<TombServiceItemData> payload, String id) {
        for (TombServiceItemData data : payload) {
            if (id.equals(data.getId())) {
                return data;
            }
        }
        return null;
    }

    private static DriverManagerDataSource h2DataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:h2:mem:park_service_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }

    private void createSchema() {
        parkJdbc.execute("CREATE TABLE t_sfi_config_item_node (id VARCHAR(32) PRIMARY KEY, "
                + "item_type VARCHAR(4), is_validate VARCHAR(2), bill_type VARCHAR(8), "
                + "bill_sub_type VARCHAR(8), item_sort_id_array VARCHAR(64))");
        parkJdbc.execute("CREATE TABLE t_sfi_code_service_type (id BIGINT PRIMARY KEY, "
                + "name VARCHAR(64), is_validate VARCHAR(2), create_date TIMESTAMP)");
        parkJdbc.execute("CREATE TABLE t_sfi_code_service (id BIGINT PRIMARY KEY, "
                + "service_type_id BIGINT, name VARCHAR(128), price DECIMAL(12,2), "
                + "index_no INT, is_validate VARCHAR(2), create_date TIMESTAMP, modify_date TIMESTAMP)");
    }

    private void seedData() {
        // node 经 item_sort_id_array 指向专属服务类型：101 代客祭扫（子类型12→订单类型6）、
        // 102 盆花租摆（子类型1→订单类型7）、103 绿化报修（无映射应过滤）
        parkJdbc.update("INSERT INTO t_sfi_config_item_node (id, item_type, is_validate, bill_type, "
                + "bill_sub_type, item_sort_id_array) VALUES ('N1', '1', '1', '1', '12', '1')");
        parkJdbc.update("INSERT INTO t_sfi_config_item_node (id, item_type, is_validate, bill_type, "
                + "bill_sub_type, item_sort_id_array) VALUES ('N2', '1', '1', '1', '1', '2')");
        parkJdbc.update("INSERT INTO t_sfi_config_item_node (id, item_type, is_validate, bill_type, "
                + "bill_sub_type, item_sort_id_array) VALUES ('N3', '1', '1', '1', '2', '3')");

        parkJdbc.update("INSERT INTO t_sfi_code_service_type (id, name, is_validate) VALUES (1, '代客祭扫', '1')");
        parkJdbc.update("INSERT INTO t_sfi_code_service_type (id, name, is_validate) VALUES (2, '盆花租摆', '1')");
        parkJdbc.update("INSERT INTO t_sfi_code_service_type (id, name, is_validate) VALUES (3, '绿化报修', '1')");

        parkJdbc.update("INSERT INTO t_sfi_code_service (id, service_type_id, name, price, index_no, "
                + "is_validate, create_date) VALUES (101, 1, '代客祭扫服务', 88.00, 1, '1', {ts '2024-01-01 00:00:00'})");
        parkJdbc.update("INSERT INTO t_sfi_code_service (id, service_type_id, name, price, index_no, "
                + "is_validate, create_date) VALUES (102, 2, '盆花租摆服务', 128.00, 2, '1', {ts '2024-01-02 00:00:00'})");
        parkJdbc.update("INSERT INTO t_sfi_code_service (id, service_type_id, name, price, index_no, "
                + "is_validate, create_date) VALUES (103, 3, '绿化报修服务', 50.00, 3, '1', {ts '2024-01-03 00:00:00'})");
    }
}
