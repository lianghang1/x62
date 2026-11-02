package com.scaffold.module.cemlink;

import com.scaffold.module.cemlink.client.MallLinkoraClient;
import com.scaffold.module.cemlink.client.model.OrderImageData;
import com.scaffold.module.cemlink.client.model.ServiceOrderData;
import com.scaffold.module.cemlink.client.model.ServiceOrderReserveData;
import com.scaffold.module.cemlink.client.model.ServiceOrderUpdateData;
import com.scaffold.module.cemlink.down.OrderDownlinkHandler;
import com.scaffold.starter.http.HttpProperties;
import com.scaffold.starter.http.ScaffoldHttpClient;
import com.scaffold.starter.http.transport.TransportResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 售后订单下行回写：H2 模拟墓园库直测核心分支
 *
 * @author lianghang
 */
public class OrderDownlinkTest {

    private JdbcTemplate parkJdbc;
    private RecordingMallLinkoraClient recordingMallClient;
    private OrderDownlinkHandler handler;

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
        Path uploadDir = Files.createTempDirectory("cemlink-upload");
        handler = new OrderDownlinkHandler(parkJdbc, recordingMallClient, stubHttpClient(), uploadDir.toString());
    }

    /** 图片下载桩：测试未覆盖图片路径，任意 URL 返回 404 空体 */
    private ScaffoldHttpClient stubHttpClient() {
        return new ScaffoldHttpClient(request -> new TransportResponse(404, Collections.emptyMap(), new byte[0]),
                new HttpProperties(), null);
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void maintenanceFeeOrderCreatesMaintenanceAndPayment() {
        handler.handle(Collections.singletonList(order("9001", "3", "1", 36000L, 1, "1001", null)));

        // 维护费记录
        assertEquals(Integer.valueOf(1), parkJdbc.queryForObject(
                "SELECT maint_index FROM t_sfi_bus_maintenance WHERE order_id = '9001'", Integer.class));
        assertEquals(Double.valueOf(360d), parkJdbc.queryForObject(
                "SELECT maint_fee FROM t_sfi_bus_maintenance WHERE order_id = '9001'", Double.class));
        // 结算主表 / 业务服务表 / 详情表
        Long paymentId = parkJdbc.queryForObject(
                "SELECT id FROM t_sfi_payment WHERE pay_no = 'SN9001'", Long.class);
        assertEquals(Double.valueOf(360d), parkJdbc.queryForObject(
                "SELECT pay_sum FROM t_sfi_payment WHERE id = ?", Double.class, paymentId));
        Long businessServiceId = parkJdbc.queryForObject(
                "SELECT id FROM t_sfi_business_service WHERE bill_type = '7' AND bill_id = "
                        + "(SELECT id FROM t_sfi_bus_maintenance WHERE order_id = '9001')", Long.class);
        Long detailCount = parkJdbc.queryForObject(
                "SELECT COUNT(1) FROM t_sfi_payment_detail WHERE payment_id = ? AND bus_service_id = ?",
                Long.class, paymentId, businessServiceId);
        assertEquals(Long.valueOf(1L), detailCount);
        // 业务版本递增
        assertEquals(Integer.valueOf(1), parkJdbc.queryForObject(
                "SELECT collect_version FROM t_sfi_business WHERE id = 1001", Integer.class));
        assertEquals(Integer.valueOf(1), parkJdbc.queryForObject(
                "SELECT pay_version FROM t_sfi_business WHERE id = 1001", Integer.class));
        // 采集记录 + 回传
        assertEquals("1", parkJdbc.queryForObject(
                "SELECT result FROM t_sfi_bus_collection_order_service WHERE order_no = 'NO9001'", String.class));
        assertEquals(1, recordingMallClient.topics.size());
        assertEquals("cemetery-tomb-order-update", recordingMallClient.topics.get(0));
        @SuppressWarnings("unchecked")
        List<ServiceOrderUpdateData> updates = (List<ServiceOrderUpdateData>) recordingMallClient.payloads.get(0);
        assertEquals("9001", updates.get(0).getId());
        assertEquals("3", updates.get(0).getOrderType());
    }

    @Test
    public void repairOrderCreatesAfterServiceChain() {
        ServiceOrderData order = order("9002", "2", "1", 5000L, 1, "1001", null);
        ServiceOrderReserveData reserve = new ServiceOrderReserveData();
        reserve.setServiceId("5001");
        reserve.setServiceName("保洁服务");
        reserve.setPrice(5000L);
        reserve.setQty(1);
        order.setServices(Collections.singletonList(reserve));

        handler.handle(Collections.singletonList(order));

        // 售后主表 / sub 表 / 业务服务表链路
        Long serviceId = parkJdbc.queryForObject(
                "SELECT id FROM t_sfi_service WHERE order_id = '9002'", Long.class);
        Long subId = parkJdbc.queryForObject(
                "SELECT id FROM t_sfi_service_sub WHERE service_id = ? AND category = '7'", Long.class, serviceId);
        assertEquals("2", parkJdbc.queryForObject(
                "SELECT source_type FROM t_sfi_service_sub WHERE id = ?", String.class, subId));
        Long businessServiceCount = parkJdbc.queryForObject(
                "SELECT COUNT(1) FROM t_sfi_business_service WHERE bill_id = ? AND bill_type = '6' AND bill_sub_type = '7'",
                Long.class, serviceId);
        assertEquals(Long.valueOf(1L), businessServiceCount);
        assertEquals("保洁服务", parkJdbc.queryForObject(
                "SELECT name FROM t_sfi_business_service WHERE bill_id = ?", String.class, serviceId));
        // 明细ID回填
        assertEquals("5001", parkJdbc.queryForObject(
                "SELECT detail_item_id_array FROM t_sfi_service_sub WHERE id = ?", String.class, subId));
        assertEquals("1", parkJdbc.queryForObject(
                "SELECT result FROM t_sfi_bus_collection_order_service WHERE order_no = 'NO9002'", String.class));
    }

    @Test
    public void repairOrderWithCompleteTimeCreatesWork() {
        ServiceOrderData order = order("9003", "2", "1", 5000L, 1, "1001", null);
        java.util.Date completeTime = new java.util.Date();
        order.setCompleteTime(completeTime);
        order.setCompleteUser("客户");
        order.setCompleteResult("满意");

        handler.handle(Collections.singletonList(order));

        Long serviceId = parkJdbc.queryForObject(
                "SELECT id FROM t_sfi_service WHERE order_id = '9003'", Long.class);
        // 发单标记 + 工单
        assertEquals("2", parkJdbc.queryForObject(
                "SELECT work_flag FROM t_sfi_service WHERE id = ?", String.class, serviceId));
        Long subId = parkJdbc.queryForObject(
                "SELECT id FROM t_sfi_service_sub WHERE service_id = ?", Long.class, serviceId);
        assertEquals("1", parkJdbc.queryForObject(
                "SELECT work_flag FROM t_sfi_service_sub WHERE id = ?", String.class, subId));
        assertEquals("2", parkJdbc.queryForObject(
                "SELECT work_status FROM t_sfi_service_sub WHERE id = ?", String.class, subId));
        Long workId = parkJdbc.queryForObject(
                "SELECT id FROM t_sfi_work WHERE bill_id = ? AND bill_type = '6'", Long.class, serviceId);
        assertEquals("2", parkJdbc.queryForObject(
                "SELECT work_status FROM t_sfi_work WHERE id = ?", String.class, workId));
        assertEquals("满意", parkJdbc.queryForObject(
                "SELECT check_remark FROM t_sfi_work WHERE id = ?", String.class, workId));
        // 验收记录
        Long workCheckCount = parkJdbc.queryForObject(
                "SELECT COUNT(1) FROM t_sfi_work_check WHERE work_id = ? AND check_status = '1'",
                Long.class, workId);
        assertEquals(Long.valueOf(1L), workCheckCount);
    }

    @Test
    public void earlyLinkmanCancelInvalidatesRecords() {
        handler.handle(Collections.singletonList(order("9004", "4", "3", null, 1, "1001", null)));

        assertEquals("0", parkJdbc.queryForObject(
                "SELECT is_validate FROM t_sfi_early_linkman WHERE order_id = '9004'", String.class));
        assertEquals("0", parkJdbc.queryForObject(
                "SELECT is_validate FROM t_sfi_early_linkman_detail WHERE early_id = 5001", String.class));
    }

    @Test
    public void depositRenewCreatesDepositAndItem() {
        handler.handle(Collections.singletonList(order("9005", "9", "1", 30000L, 1, null, "2001")));

        // 寄存记录：首期 index=1
        Long depositId = parkJdbc.queryForObject(
                "SELECT id FROM t_sfi_ashes_bus_deposit WHERE order_id = '9005'", Long.class);
        assertEquals(Integer.valueOf(1), parkJdbc.queryForObject(
                "SELECT deposit_index FROM t_sfi_ashes_bus_deposit WHERE id = ?", Integer.class, depositId));
        assertEquals(Integer.valueOf(1), parkJdbc.queryForObject(
                "SELECT deposit_year FROM t_sfi_ashes_bus_deposit WHERE id = ?", Integer.class, depositId));
        assertEquals("1", parkJdbc.queryForObject(
                "SELECT last_flag FROM t_sfi_ashes_bus_deposit WHERE id = ?", String.class, depositId));
        // 续存费记录
        assertEquals("0010", parkJdbc.queryForObject(
                "SELECT bill_type FROM t_sfi_ashes_bus_item WHERE bill_id = ?", String.class, depositId));
        assertEquals(Double.valueOf(300d), parkJdbc.queryForObject(
                "SELECT deposit_fee FROM t_sfi_ashes_bus_deposit WHERE id = ?", Double.class, depositId));
        assertEquals("1", parkJdbc.queryForObject(
                "SELECT result FROM t_sfi_bus_collection_order_service WHERE order_no = 'NO9005'", String.class));
    }

    private ServiceOrderData order(String id, String orderType, String orderStatus, Long amount,
                                   Integer qty, String tombId, String depositId) {
        ServiceOrderData order = new ServiceOrderData();
        order.setId(id);
        order.setOrderNo("NO" + id);
        order.setOrderType(orderType);
        order.setOrderStatus(orderStatus);
        order.setAmount(amount);
        order.setQty(qty);
        order.setTombId(tombId);
        order.setDepositId(depositId);
        order.setPaySerialNo("SN" + id);
        order.setPayTime(new java.util.Date());
        order.setDate(new java.util.Date());
        return order;
    }

    private static DriverManagerDataSource h2DataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:h2:mem:park_order_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }

    private void createSchema() {
        parkJdbc.execute("CREATE TABLE t_sfi_business (id BIGINT PRIMARY KEY, tomb_id BIGINT, "
                + "business_no VARCHAR(32), collect_version INT, pay_version INT, modify_date TIMESTAMP, "
                + "is_validate VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_tomb (id BIGINT PRIMARY KEY, layer_code VARCHAR(64), "
                + "status_id INT, basic_price DOUBLE)");
        parkJdbc.execute("CREATE TABLE t_sfi_bus_linkman (id BIGINT PRIMARY KEY, business_id BIGINT, "
                + "is_validate VARCHAR(2), name VARCHAR(64), sex VARCHAR(2), mobile_no VARCHAR(20), "
                + "phone_no VARCHAR(20), address_province VARCHAR(16), address_city VARCHAR(16), "
                + "address_county VARCHAR(16), address_detail VARCHAR(128), address_full VARCHAR(128))");
        parkJdbc.execute("CREATE TABLE t_sfi_bus_maintenance (id BIGINT PRIMARY KEY, business_id BIGINT, "
                + "create_date TIMESTAMP, create_user INT, is_validate VARCHAR(2), maint_year INT, start_date TIMESTAMP, end_date TIMESTAMP, "
                + "maint_fee DOUBLE, service_fee DOUBLE, maint_index INT, pay_status VARCHAR(2), "
                + "remark VARCHAR(128), order_id VARCHAR(32))");
        parkJdbc.execute("CREATE TABLE t_sfi_business_service (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, business_id BIGINT, bill_id BIGINT, bill_sub_id BIGINT, "
                + "bill_type VARCHAR(8), bill_sub_type VARCHAR(8), service_id VARCHAR(32), name VARCHAR(64), "
                + "qty DOUBLE, price DOUBLE, `sum` DOUBLE, pay_status VARCHAR(2), pay_date TIMESTAMP, "
                + "invoice_status VARCHAR(2), invoice_date TIMESTAMP)");
        parkJdbc.execute("CREATE TABLE t_sfi_payment (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, business_id BIGINT, pay_no VARCHAR(32), "
                + "business_type VARCHAR(2), business_source VARCHAR(2), layer_code VARCHAR(64), "
                + "pay_date TIMESTAMP, pay_user VARCHAR(32), pay_user_id INT, pay_sum DOUBLE, "
                + "discount_sum DOUBLE, fact_sum DOUBLE, invoice_no VARCHAR(64), payment_type VARCHAR(2), "
                + "print_flag VARCHAR(2), refund_flag VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_payment_detail (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, payment_id BIGINT, bus_service_id BIGINT, "
                + "retail_detail_id BIGINT, deposit_item_id BIGINT, fact_sum DOUBLE)");
        parkJdbc.execute("CREATE TABLE t_sfi_payment_method (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, payment_id BIGINT, method_id INT, fact_sum DOUBLE)");
        parkJdbc.execute("CREATE TABLE t_sfi_pay_invoice (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, business_id BIGINT, business_type VARCHAR(2), "
                + "business_source VARCHAR(2), invoice_date TIMESTAMP, invoice_user VARCHAR(32), "
                + "invoice_user_id INT, invoice_sum DOUBLE, discount_sum DOUBLE, fact_sum DOUBLE, "
                + "invoice_no VARCHAR(64), payment_type VARCHAR(2), invoice_unit VARCHAR(64), "
                + "print_flag VARCHAR(2), refund_flag VARCHAR(2), return_tomb_flag VARCHAR(2), "
                + "invoice_code VARCHAR(32), is_tax VARCHAR(2), tax_type VARCHAR(8), payment_id BIGINT)");
        parkJdbc.execute("CREATE TABLE t_sfi_pay_invoice_detail (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, invoice_id BIGINT, bus_service_id BIGINT, "
                + "retail_detail_id BIGINT, deposit_item_id BIGINT, fact_sum DOUBLE)");
        parkJdbc.execute("CREATE TABLE t_sfi_payment_invoice_tax (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, business_id BIGINT, business_type VARCHAR(2), "
                + "business_no VARCHAR(32), serial_no VARCHAR(64), status VARCHAR(2), total_amount DOUBLE, "
                + "total_tax DOUBLE, invoice_time TIMESTAMP, invoice_code VARCHAR(32), invoice_no VARCHAR(64), "
                + "pdf_url VARCHAR(256), payment_id BIGINT)");
        parkJdbc.execute("CREATE TABLE t_sfi_service (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, modify_date TIMESTAMP, modify_user INT, "
                + "business_id BIGINT, service_no VARCHAR(32), pay_status VARCHAR(2), work_flag VARCHAR(2), "
                + "apply_date TIMESTAMP, register_user VARCHAR(32), register_user_id INT, remark VARCHAR(256), "
                + "order_id VARCHAR(32), order_type VARCHAR(4))");
        parkJdbc.execute("CREATE TABLE t_sfi_service_sub (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, service_id BIGINT, category VARCHAR(8), "
                + "pay_status VARCHAR(2), work_flag VARCHAR(2), send_flag VARCHAR(2), work_status VARCHAR(2), "
                + "message_flag VARCHAR(2), apply_date TIMESTAMP, finish_date TIMESTAMP, start_time TIMESTAMP, "
                + "end_time TIMESTAMP, service_time INT, source_type VARCHAR(2), register_user VARCHAR(32), "
                + "register_user_id INT, remark VARCHAR(256), detail_item_id_array VARCHAR(128), "
                + "detail_item_name_array VARCHAR(256))");
        parkJdbc.execute("CREATE TABLE t_sfi_service_linkman (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_user INT, create_date TIMESTAMP, service_id BIGINT, name VARCHAR(64), sex VARCHAR(2), "
                + "address_province VARCHAR(16), address_city VARCHAR(16), address_county VARCHAR(16), "
                + "address_detail VARCHAR(128), address_full VARCHAR(128), phone_no VARCHAR(20), mobile_no VARCHAR(20))");
        parkJdbc.execute("CREATE TABLE t_sfi_service_sub_flie (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, service_sub_id BIGINT, file_path VARCHAR(256), "
                + "file_name VARCHAR(128), file_size DOUBLE, file_suffix VARCHAR(16))");
        parkJdbc.execute("CREATE TABLE t_sfi_work (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, register_user VARCHAR(32), register_user_id INT, "
                + "business_id BIGINT, bill_id BIGINT, bill_type VARCHAR(8), bill_sub_id BIGINT, "
                + "bill_sub_type VARCHAR(8), work_status VARCHAR(2), work_date TIMESTAMP, check_date TIMESTAMP, "
                + "finish_date TIMESTAMP, check_status VARCHAR(2), check_remark VARCHAR(256), operate_version INT)");
        parkJdbc.execute("CREATE TABLE t_sfi_work_check (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, work_id BIGINT, operate_date TIMESTAMP, "
                + "operate_user VARCHAR(32), operate_user_id VARCHAR(32), remark VARCHAR(256), "
                + "check_status VARCHAR(2), operate_version INT)");
        parkJdbc.execute("CREATE TABLE t_sfi_work_check_flie (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, work_check_id BIGINT, file_path VARCHAR(256), "
                + "file_name VARCHAR(128), file_size DOUBLE, file_suffix VARCHAR(16))");
        parkJdbc.execute("CREATE TABLE t_sfi_early_linkman (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, modify_date TIMESTAMP, modify_user INT, "
                + "buy_flag VARCHAR(2), track_flag VARCHAR(2), push_status VARCHAR(2), order_id VARCHAR(32))");
        parkJdbc.execute("CREATE TABLE t_sfi_early_linkman_detail (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, modify_date TIMESTAMP, modify_user INT, early_id BIGINT, name VARCHAR(64), mobile_no VARCHAR(20), "
                + "arrive_time TIMESTAMP, user_name VARCHAR(64), register_date TIMESTAMP, register_user VARCHAR(32), "
                + "register_user_id INT, remark VARCHAR(256))");
        parkJdbc.execute("CREATE TABLE t_sfi_bury_book (id BIGINT PRIMARY KEY, business_id BIGINT, user_id BIGINT, "
                + "is_validate VARCHAR(2), create_date TIMESTAMP, create_user INT, order_id VARCHAR(32), "
                + "order_type VARCHAR(4), pay_status VARCHAR(2), msg_send_flag VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_bury_book_detail (id BIGINT PRIMARY KEY, bury_book_id BIGINT, "
                + "linkman VARCHAR(64), mobile_no VARCHAR(20), book_date TIMESTAMP, register_date TIMESTAMP, "
                + "register_user VARCHAR(32), register_user_id INT, remark VARCHAR(256), is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT)");
        parkJdbc.execute("CREATE TABLE t_sfi_bus_user (id BIGINT PRIMARY KEY, pre_bury_flag VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_booking (id BIGINT PRIMARY KEY, valid_flag VARCHAR(2), "
                + "business_flag VARCHAR(2), base_sum DOUBLE, booking_no VARCHAR(32), user_name VARCHAR(128), "
                + "book_day INT, apply_date TIMESTAMP, start_time TIMESTAMP, end_time TIMESTAMP, remark VARCHAR(256), "
                + "is_validate VARCHAR(2), create_date TIMESTAMP, create_user INT, order_id VARCHAR(32), "
                + "order_type VARCHAR(4))");
        parkJdbc.execute("CREATE TABLE t_sfi_booking_tomb (id BIGINT PRIMARY KEY, booking_id BIGINT, tomb_id BIGINT, "
                + "tomb_status_id VARCHAR(2), is_validate VARCHAR(2), create_date TIMESTAMP, create_user INT)");
        parkJdbc.execute("CREATE TABLE t_sfi_book_linkman (id BIGINT PRIMARY KEY, booking_id BIGINT, name VARCHAR(64), "
                + "mobile_no VARCHAR(20), is_validate VARCHAR(2), create_date TIMESTAMP, create_user INT)");
        parkJdbc.execute("CREATE TABLE t_sfi_book_user (id BIGINT PRIMARY KEY, booking_id BIGINT, name VARCHAR(64), "
                + "sex VARCHAR(2), is_validate VARCHAR(2), create_date TIMESTAMP, create_user INT)");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_business (id BIGINT PRIMARY KEY, business_no VARCHAR(32), "
                + "modify_date TIMESTAMP, is_validate VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_grid (id VARCHAR(32) PRIMARY KEY, business_id VARCHAR(32), "
                + "is_validate VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_deposit (id BIGINT PRIMARY KEY, create_date TIMESTAMP, "
                + "create_user INT, is_validate VARCHAR(2), business_id VARCHAR(32), bus_grid_id VARCHAR(32), "
                + "deposit_index INT, deposit_year INT, deposit_fee DOUBLE, start_date TIMESTAMP, end_date TIMESTAMP, "
                + "pay_status VARCHAR(2), pay_date TIMESTAMP, last_flag VARCHAR(2), remark VARCHAR(128), "
                + "sale_office VARCHAR(8), order_id VARCHAR(32))");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_item (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, business_id VARCHAR(32), bus_grid_id VARCHAR(32), "
                + "bill_id BIGINT, bill_type VARCHAR(8), item_id VARCHAR(32), name VARCHAR(64), `year` INT, qty INT, "
                + "price DOUBLE, `sum` DOUBLE, start_date TIMESTAMP, end_date TIMESTAMP, sale_office VARCHAR(8), "
                + "pay_status VARCHAR(2), pay_date TIMESTAMP, invoice_status VARCHAR(2), invoice_date TIMESTAMP)");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_bus_after_service (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, modify_date TIMESTAMP, modify_user INT, business_id BIGINT, "
                + "status VARCHAR(2), pay_status VARCHAR(2), register_date TIMESTAMP, register_user VARCHAR(32), "
                + "register_user_id INT, finish_date TIMESTAMP, send_date TIMESTAMP, check_date TIMESTAMP, "
                + "remark VARCHAR(256), order_id VARCHAR(32), order_type VARCHAR(4))");
        parkJdbc.execute("CREATE TABLE t_sfi_ashes_customer_order (id BIGINT PRIMARY KEY, name VARCHAR(64), "
                + "mobile_no VARCHAR(20), certificate_type VARCHAR(2), certificate_no VARCHAR(32), "
                + "address_province VARCHAR(16), address_city VARCHAR(16), address_county VARCHAR(16), "
                + "address_detail VARCHAR(128), address_full VARCHAR(128), user_name VARCHAR(64), user_sex VARCHAR(2), "
                + "user_certificate_type VARCHAR(2), user_certificate_no VARCHAR(32), status VARCHAR(2), "
                + "remark VARCHAR(256), is_validate VARCHAR(2), create_date TIMESTAMP, create_user INT, order_id VARCHAR(32))");
        parkJdbc.execute("CREATE TABLE t_sfi_retail (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, retail_no VARCHAR(32), business_no VARCHAR(32), "
                + "bookdelivery_time TIMESTAMP, sum_money DOUBLE, fact_money DOUBLE, discount_money DOUBLE, "
                + "pre_sum_money DOUBLE, remark VARCHAR(256), operate_user VARCHAR(32), operate_user_id INT, "
                + "operate_date TIMESTAMP, confirm_flag VARCHAR(2), pay_status VARCHAR(2), retail_type VARCHAR(2), "
                + "business_source VARCHAR(2), provide_flag VARCHAR(2), order_id VARCHAR(32))");
        parkJdbc.execute("CREATE TABLE t_sfi_retail_detail (id BIGINT PRIMARY KEY, is_validate VARCHAR(2), "
                + "create_date TIMESTAMP, create_user INT, retail_id BIGINT, goods_id VARCHAR(32), price DOUBLE, "
                + "qty DOUBLE, fact_price DOUBLE, discount_price DOUBLE, sum_price DOUBLE, pay_status VARCHAR(2), "
                + "invoice_status VARCHAR(2), store_id VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_bus_collection_order_service (id BIGINT PRIMARY KEY, bus_id INT, "
                + "order_id INT, order_no VARCHAR(32), order_status VARCHAR(4), pay_time TIMESTAMP, "
                + "pay_serial_no VARCHAR(64), order_date TIMESTAMP, result VARCHAR(2), services_id INT, "
                + "services_name VARCHAR(64), service_price BIGINT, service_qty INT)");
        parkJdbc.execute("CREATE TABLE t_sfi_code_method (id BIGINT PRIMARY KEY, name VARCHAR(32))");
    }

    private void seedData() {
        parkJdbc.update("INSERT INTO t_sfi_business (id, tomb_id, business_no, collect_version, pay_version, "
                + "is_validate) VALUES (1001, 7001, 'BZ1001', 0, 0, '1')");
        parkJdbc.update("INSERT INTO t_sfi_tomb (id, layer_code, status_id, basic_price) VALUES (7001, 'A-01', 2, 12800.0)");
        parkJdbc.update("INSERT INTO t_sfi_bus_linkman (id, business_id, is_validate, name, mobile_no) "
                + "VALUES (3001, 1001, '1', '张三', '13800000000')");
        // 预约看墓取消用例
        parkJdbc.update("INSERT INTO t_sfi_early_linkman (id, is_validate, buy_flag, track_flag, push_status, order_id) "
                + "VALUES (5001, '1', '0', '0', '0', '9004')");
        parkJdbc.update("INSERT INTO t_sfi_early_linkman_detail (id, early_id, is_validate, name) VALUES (6001, 5001, '1', '张三')");
        // 寄存续费用例
        parkJdbc.update("INSERT INTO t_sfi_ashes_business (id, business_no, is_validate) VALUES (2001, 'BZ2001', '1')");
        parkJdbc.update("INSERT INTO t_sfi_ashes_bus_grid (id, business_id, is_validate) VALUES ('G1', '2001', '1')");
        parkJdbc.update("INSERT INTO t_sfi_code_method (id, name) VALUES (1, '微信支付')");
    }
}
