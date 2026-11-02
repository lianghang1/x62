package com.scaffold.cemlink.down;

import com.scaffold.cemlink.client.model.ServiceOrderData;
import com.scaffold.cemlink.client.model.ServiceOrderInvoiceData;
import com.scaffold.cemlink.client.model.ServiceOrderReserveData;
import com.scaffold.cemlink.contract.OrderType;
import com.scaffold.starter.http.ScaffoldHttpClient;
import com.scaffold.starter.http.transport.TransportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单下行写库：原 TombOrderDataCollectionServiceImpl / BaseOrderDataCollectionService 平移
 *
 * @author lianghang
 */
public class OrderDownlinkWriter {

    private static final Logger log = LoggerFactory.getLogger(OrderDownlinkWriter.class);

    private static final int CREATE_USER_ID = 9999;
    private static final String CREATE_USER = "系统";
    private static final String PAY_USER = "网络";
    private static final String REMAEK = "（来自网络【公众号】下单）";
    /** 工单状态：工程验收 */
    private static final String WORK_STATUS_ACCEPTED = "2";
    /** 全款 */
    private static final String PAY_STATUS_FULL = "2";
    /** 已结算 */
    private static final String PAY_EASY_SETTLED = "2";

    private final JdbcTemplate jdbcTemplate;
    private final ScaffoldHttpClient scaffoldHttpClient;
    private final String uploadPath;

    public OrderDownlinkWriter(@Qualifier("cemlinkParkJdbc") JdbcTemplate jdbcTemplate,
                               ScaffoldHttpClient scaffoldHttpClient, String uploadPath) {
        this.jdbcTemplate = jdbcTemplate;
        this.scaffoldHttpClient = scaffoldHttpClient;
        this.uploadPath = uploadPath;
    }

    // ==================== 分支入口 ====================

    /** 原 saveServiceInfo 分发平移：按订单类型写对应业务表 */
    public void saveServiceInfo(ServiceOrderData orderData, Date operateTime, Long businessId, Long tombId,
                                Long linkmanId, Long ashesBusinessId) {
        if (orderData.getOrderType() == null || orderData.getOrderType().isEmpty()) {
            throw new RuntimeException("同步订单类型不能为空");
        }

        if (OrderType.商城购物.getCode().equals(orderData.getOrderType())) {
            saveRetailInfo(orderData, operateTime, "7".equals(orderData.getOrderStatus()));

        } else if (OrderType.维护费.getCode().equals(orderData.getOrderType())) {
            saveMaintenanceInfo(businessId, tombId, orderData, operateTime, "7".equals(orderData.getOrderStatus()));

        } else if (OrderType.预约看墓.getCode().equals(orderData.getOrderType())) {
            saveEarlyLinkman(orderData, operateTime, "3".equals(orderData.getOrderStatus()));

        } else if (OrderType.预约落葬.getCode().equals(orderData.getOrderType())) {
            saveBuryBook(businessId, orderData, operateTime, "3".equals(orderData.getOrderStatus()));

        } else if (OrderType.在线报修.getCode().equals(orderData.getOrderType())) {
            String billSubType = "7";
            Map<String, String> extendData = orderData.getExtendData();
            if (extendData != null && extendData.containsKey("reason") && "42".equals(extendData.get("reason"))) {
                billSubType = "2";
            }
            saveAfterServiceInfo(businessId, tombId, linkmanId, orderData, "6", billSubType, operateTime,
                    "7".equals(orderData.getOrderStatus()));

        } else if (OrderType.盆花租摆.getCode().equals(orderData.getOrderType())) {
            saveAfterServiceInfo(businessId, tombId, linkmanId, orderData, "6", "1", operateTime,
                    "7".equals(orderData.getOrderStatus()));

        } else if (OrderType.代客祭扫.getCode().equals(orderData.getOrderType())) {
            saveAfterServiceInfo(businessId, tombId, linkmanId, orderData, "6", "12", operateTime,
                    "7".equals(orderData.getOrderStatus()));

        } else if (OrderType.寄存祭扫.getCode().equals(orderData.getOrderType())) {
            saveDepositSacrifice(ashesBusinessId, orderData, operateTime, "7".equals(orderData.getOrderStatus()));

        } else if (OrderType.描碑.getCode().equals(orderData.getOrderType())) {
            saveAfterServiceInfo(businessId, tombId, linkmanId, orderData, "6", "9", operateTime,
                    "10".equals(orderData.getOrderStatus()));

        } else if (OrderType.深度保洁.getCode().equals(orderData.getOrderType())) {
            // 原程序空实现
        } else if (OrderType.提交照片.getCode().equals(orderData.getOrderType())) {
            saveAfterServiceInfo(businessId, tombId, linkmanId, orderData, "6", "60", operateTime, false);

        } else if (OrderType.寄存续费.getCode().equals(orderData.getOrderType())) {
            saveDepositRenew(ashesBusinessId, orderData, operateTime, "7".equals(orderData.getOrderStatus()));

        } else if (OrderType.在线售墓.getCode().equals(orderData.getOrderType())) {
            saveBookTomb(orderData, operateTime);

        } else if (OrderType.寄存预约.getCode().equals(orderData.getOrderType())) {
            saveAshesDepositRegister(orderData, operateTime, "3".equals(orderData.getOrderStatus()));
        }
    }

    // ==================== 商城购物（零售单） ====================

    private void saveRetailInfo(ServiceOrderData orderData, Date operateTime, boolean retreat) {
        if (retreat) { // 退单
            List<Long> retailIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_retail WHERE retail_no = ? AND business_source = '2'", Long.class,
                    orderData.getOrderNo());
            if (!retailIds.isEmpty()) {
                jdbcTemplate.update("UPDATE t_sfi_retail SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                        operateTime, CREATE_USER_ID, retailIds.get(0));
            }
        } else {
            List<Long> retailIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_retail WHERE order_id = ?", Long.class, orderData.getId());
            if (retailIds.isEmpty()) { // 新增
                if (orderData.getPayTime() != null) {
                    operateTime = orderData.getPayTime();
                }
                double amount = new BigDecimal(orderData.getAmount()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).doubleValue();

                long retailId = nextId("t_sfi_retail");
                jdbcTemplate.update("INSERT INTO t_sfi_retail (id, is_validate, create_date, create_user, retail_no, "
                                + "business_no, bookdelivery_time, sum_money, fact_money, discount_money, pre_sum_money, "
                                + "remark, operate_user, operate_user_id, operate_date, confirm_flag, pay_status, "
                                + "retail_type, business_source, provide_flag, order_id) VALUES "
                                + "(?, '1', ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?, '1', '2', '1', '2', '0', ?)",
                        retailId, operateTime, CREATE_USER_ID, orderData.getOrderNo(), orderData.getOrderNo(),
                        orderData.getDate(), amount, amount, orderData.getRemark(), CREATE_USER, CREATE_USER_ID,
                        operateTime, orderData.getId());

                Long paymentId = null;
                if (amount > 0) {
                    paymentId = insertPayment(retailId, "2", orderData.getPaySerialNo(), null, amount, operateTime);
                    String payType = orderData.getPayType() != null ? orderData.getPayType().toString() : null;
                    insertPaymentMethod(paymentId, amount, operateTime, payType);
                }

                List<ServiceOrderReserveData> listData = orderData.getServices();
                if (listData != null) {
                    for (ServiceOrderReserveData data : listData) {
                        double price = new BigDecimal(data.getPrice()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).doubleValue();
                        double qty = new BigDecimal(data.getQty()).doubleValue();

                        long retailDetailId = nextId("t_sfi_retail_detail");
                        jdbcTemplate.update("INSERT INTO t_sfi_retail_detail (id, is_validate, create_date, create_user, "
                                        + "retail_id, goods_id, price, qty, fact_price, discount_price, sum_price, pay_status, "
                                        + "invoice_status, store_id) VALUES (?, '1', ?, ?, ?, ?, ?, ?, ?, 0, ?, '1', ?, '3')",
                                retailDetailId, operateTime, CREATE_USER_ID, retailId, data.getServiceId(), price, qty,
                                price, price * qty,
                                orderData.getInvoiceNo() != null && !orderData.getInvoiceNo().isEmpty() ? "1" : null);

                        if (paymentId != null) {
                            insertPaymentDetail(paymentId, null, retailDetailId, null, price * qty, operateTime);
                        }
                    }
                }

                saveOrUpdateRetailPaymentInvoice(retailId, retailId, orderData.getOrderNo(), orderData, operateTime);
            } else { // 修改
                saveOrUpdateRetailPaymentInvoice(retailIds.get(0), retailIds.get(0),
                        queryForString("SELECT retail_no FROM t_sfi_retail WHERE id = ?", retailIds.get(0)),
                        orderData, operateTime);
            }
        }
    }

    // ==================== 维护费 ====================

    private void saveMaintenanceInfo(Long businessId, Long tombId, ServiceOrderData orderData, Date operateTime, boolean retreat) {
        List<Long> maintenanceIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_bus_maintenance WHERE order_id = ?", Long.class, orderData.getId());
        if (retreat) { // 退单
            if (maintenanceIds.isEmpty()) {
                return;
            }
            long maintenanceId = maintenanceIds.get(0);
            jdbcTemplate.update("UPDATE t_sfi_bus_maintenance SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                    operateTime, CREATE_USER_ID, maintenanceId);

            // 重新排序该业务的其他有效维护记录的 maintIndex
            List<Long> validMaintIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_bus_maintenance WHERE business_id = ? AND is_validate = '1' ORDER BY maint_index ASC",
                    Long.class, businessId);
            for (int i = 0; i < validMaintIds.size(); i++) {
                jdbcTemplate.update("UPDATE t_sfi_bus_maintenance SET maint_index = ?, modify_date = ?, modify_user = ? WHERE id = ?",
                        i + 1, operateTime, CREATE_USER_ID, validMaintIds.get(i));
            }

            List<Long> businessServiceIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_business_service WHERE bill_id = ? AND bill_type = '10'", Long.class, maintenanceId);
            if (!businessServiceIds.isEmpty()) {
                jdbcTemplate.update("UPDATE t_sfi_business_service SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                        operateTime, CREATE_USER_ID, businessServiceIds.get(0));
            }

            saveOrUpdateRefundPayment(businessId, queryForString("SELECT business_no FROM t_sfi_business WHERE id = ?", businessId),
                    orderData);
        } else {
            if (maintenanceIds.isEmpty()) { // 新增
                saveMaintenanceInfoForAdd(businessId, tombId, orderData, operateTime);
            } else { // 同步更新
                saveOrUpdatePaymentInvoice(businessId, queryForString("SELECT business_no FROM t_sfi_business WHERE id = ?", businessId),
                        orderData, operateTime);
            }
        }
    }

    private void saveMaintenanceInfoForAdd(Long businessId, Long tombId, ServiceOrderData orderData, Date operateTime) {
        if (orderData.getPayTime() != null) {
            operateTime = orderData.getPayTime();
        }
        boolean renew = false; // 是否续交
        int maintIndex = 1;
        List<Long> maxIndex = jdbcTemplate.queryForList(
                "SELECT MAX(maint_index) FROM t_sfi_bus_maintenance WHERE business_id = ?", Long.class, businessId);
        if (maxIndex.get(0) != null) {
            maintIndex += maxIndex.get(0).intValue();
            renew = true;
        }

        double maintFee = new BigDecimal(orderData.getAmount()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).doubleValue();
        double serviceFee = 0d;

        double countMaintFeeAll = (maintFee + serviceFee) * orderData.getQty() / 10;
        // 订单金额 和 计算金额 相同
        if (maintFee != countMaintFeeAll) {
            maintFee = maintFee;
            serviceFee = 0d;
        } else {
            maintFee = maintFee * orderData.getQty() / 10;
            serviceFee = serviceFee * orderData.getQty() / 10;
        }

        long maintenanceId = nextId("t_sfi_bus_maintenance");
        jdbcTemplate.update("INSERT INTO t_sfi_bus_maintenance (id, business_id, create_date, create_user, is_validate, "
                        + "maint_year, maint_fee, service_fee, start_date, end_date, maint_index, pay_status, remark, order_id) "
                        + "VALUES (?, ?, ?, ?, '1', ?, ?, ?, ?, ?, ?, '1', ?, ?)",
                maintenanceId, businessId, operateTime, CREATE_USER_ID, orderData.getQty(), maintFee, serviceFee,
                orderData.getStartTime(), orderData.getEndTime(), maintIndex, REMAEK, orderData.getId());

        String layerCode = queryForString("SELECT layer_code FROM t_sfi_tomb WHERE id = ?", tombId);
        Long paymentId = insertPayment(businessId, "1", orderData.getPaySerialNo(), layerCode, maintFee, operateTime);
        // 保存结算发票主表
        Long payInvoiceId = null;
        if (orderData.getInvoices() != null && !orderData.getInvoices().isEmpty()) {
            List<ServiceOrderInvoiceData> blueInvices = filterInvoices(orderData.getInvoices(), "0");
            if (!blueInvices.isEmpty()) {
                payInvoiceId = insertPayInvoice(businessId, paymentId, "1", blueInvices.get(0), operateTime);
                jdbcTemplate.update("UPDATE t_sfi_payment SET invoice_no = ? WHERE id = ?",
                        orderData.getInvoiceNo(), paymentId);
            }
        }

        String billType = renew ? "10" : "7";
        String billTypeName = renew ? "续收维护费" : "维护费";
        Long businessServiceId = insertBusinessService(businessId, maintenanceId, billType, null, billTypeName, null, null,
                maintFee, 1d, operateTime);

        if (payInvoiceId != null) {
            jdbcTemplate.update("UPDATE t_sfi_business_service SET invoice_date = ?, invoice_status = '1' WHERE id = ?",
                    operateTime, businessServiceId);
        }

        // 保存结算详情表
        insertPaymentDetail(paymentId, businessServiceId, null, null, maintFee, operateTime);

        // 保存结算发票详情表
        if (payInvoiceId != null) {
            insertPayInvoiceDetail(payInvoiceId, businessServiceId, null, null, maintFee, operateTime);
        }

        String payType = orderData.getPayType() != null ? orderData.getPayType().toString() : null;
        insertPaymentMethod(paymentId, maintFee, operateTime, payType);

        jdbcTemplate.update("UPDATE t_sfi_business SET collect_version = IFNULL(collect_version, 0) + 1, "
                        + "pay_version = IFNULL(pay_version, 0) + 1, modify_date = ? WHERE id = ?",
                operateTime, businessId);
    }

    // ==================== 预约看墓 ====================

    private void saveEarlyLinkman(ServiceOrderData orderData, Date operateTime, boolean cancel) {
        if (cancel) {
            List<Long> linkmanIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_early_linkman WHERE order_id = ?", Long.class, orderData.getId());
            if (!linkmanIds.isEmpty()) {
                long earlyId = linkmanIds.get(0);
                jdbcTemplate.update("UPDATE t_sfi_early_linkman SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                        operateTime, CREATE_USER_ID, earlyId);

                List<Long> detailIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_early_linkman_detail WHERE early_id = ? AND is_validate = '1'",
                        Long.class, earlyId);
                for (Long detailId : detailIds) {
                    jdbcTemplate.update("UPDATE t_sfi_early_linkman_detail SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                            operateTime, CREATE_USER_ID, detailId);
                }
            }
        } else {
            String remark = orderData.getRemark();
            Map<String, String> extendData = orderData.getExtendData();
            String userName = null;
            if (extendData != null) {
                if (extendData.containsKey("intentionType")) {
                    String intentionType = "意向墓型：" + extendData.get("intentionType");
                    remark = remark != null && !remark.isEmpty() ? remark + "；" + intentionType : intentionType;
                }
                if (extendData.containsKey("deadNames")) {
                    userName = extendData.get("deadNames");
                }
            }

            long earlyId = nextId("t_sfi_early_linkman");
            jdbcTemplate.update("INSERT INTO t_sfi_early_linkman (id, buy_flag, track_flag, push_status, is_validate, "
                            + "create_date, create_user, order_id) VALUES (?, '0', '0', '0', '1', ?, ?, ?)",
                    earlyId, operateTime, CREATE_USER_ID, orderData.getId());

            jdbcTemplate.update("INSERT INTO t_sfi_early_linkman_detail (id, early_id, name, mobile_no, arrive_time, "
                            + "user_name, register_date, register_user, register_user_id, remark, is_validate, create_date, create_user) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, '系统', ?, ?, '1', ?, ?)",
                    nextId("t_sfi_early_linkman_detail"), earlyId, orderData.getName(), orderData.getPhone(),
                    orderData.getDate(), userName, operateTime, CREATE_USER_ID, remark, operateTime, CREATE_USER_ID);
        }
    }

    // ==================== 预约落葬 ====================

    private void saveBuryBook(Long businessId, ServiceOrderData orderData, Date operateTime, boolean cancel) {
        if (cancel) {
            List<Long> buryBookIds = jdbcTemplate.queryForList(
                    "SELECT id, user_id FROM t_sfi_bury_book WHERE order_id = ? AND is_validate = '1'", Long.class, orderData.getId());
            if (!buryBookIds.isEmpty()) {
                jdbcTemplate.update("UPDATE t_sfi_bury_book SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                        operateTime, CREATE_USER_ID, buryBookIds.get(0));
                // 恢复使用人预约落葬标记
                jdbcTemplate.update("UPDATE t_sfi_bus_user SET pre_bury_flag = '0' WHERE id = "
                        + "(SELECT user_id FROM t_sfi_bury_book WHERE id = ?)", buryBookIds.get(0));
            }
        } else {
            Long busUserId = null;
            Map<String, String> extendData = orderData.getExtendData();
            if (extendData != null && extendData.containsKey("userId")) {
                List<Long> busUsers = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_bus_user WHERE id = ?", Long.class, Long.parseLong(extendData.get("userId")));
                if (!busUsers.isEmpty()) {
                    busUserId = busUsers.get(0);
                    jdbcTemplate.update("UPDATE t_sfi_bus_user SET pre_bury_flag = '1' WHERE id = ?", busUserId);
                }
            }

            long buryBookId = nextId("t_sfi_bury_book");
            jdbcTemplate.update("INSERT INTO t_sfi_bury_book (id, business_id, user_id, is_validate, create_date, create_user, "
                            + "order_id, order_type, pay_status, msg_send_flag) VALUES (?, ?, ?, '1', ?, ?, ?, ?, '0', '0')",
                    buryBookId, businessId, busUserId, operateTime, CREATE_USER_ID, orderData.getId(),
                    OrderType.预约落葬.getCode());

            jdbcTemplate.update("INSERT INTO t_sfi_bury_book_detail (id, bury_book_id, linkman, mobile_no, book_date, "
                            + "register_date, register_user, register_user_id, remark, is_validate, create_date, create_user) "
                            + "VALUES (?, ?, ?, ?, ?, ?, '系统', ?, ?, '1', ?, ?)",
                    nextId("t_sfi_bury_book_detail"), buryBookId, orderData.getName(), orderData.getPhone(),
                    orderData.getDate(), operateTime, CREATE_USER_ID, orderData.getRemark(), operateTime, CREATE_USER_ID);
        }
    }

    // ==================== 售后工单族（报修/盆花租摆/代客祭扫/描碑/照片验收） ====================

    private void saveAfterServiceInfo(Long businessId, Long tombId, Long linkmanId, ServiceOrderData orderData,
                                      String billType, String billSubType, Date operateTime, boolean retreat) {
        List<Long> serviceIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_service WHERE order_id = ? AND service_no = ? AND is_validate = '1'",
                Long.class, orderData.getId(), orderData.getOrderNo());
        if (retreat) { // 退单
            saveAfterServiceInfoForRetreat(businessId, billType, billSubType, operateTime, serviceIds, orderData);
        } else {
            if (serviceIds.isEmpty()) { // 新增
                saveAfterServiceInfoForAdd(businessId, tombId, linkmanId, orderData, billType, billSubType, operateTime);
            } else { // 同步更新
                saveAfterServiceInfoForUpdate(businessId, tombId, orderData, billType, billSubType, operateTime, serviceIds.get(0));
            }
        }
    }

    private void saveAfterServiceInfoForAdd(Long businessId, Long tombId, Long linkmanId, ServiceOrderData orderData,
                                            String billType, String billSubType, Date operateTime) {
        double amount = 0;
        if (orderData.getAmount() != null && orderData.getAmount() > 0) {
            amount = new BigDecimal(orderData.getAmount()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).doubleValue();
        }
        if (orderData.getPayTime() != null) {
            operateTime = orderData.getPayTime();
        }

        long serviceId = insertService(businessId, orderData, operateTime);

        // 保存售后联系人表
        insertServiceLinkman(serviceId, linkmanId, operateTime);

        // 保存售后sub表
        long subId = insertServiceSub(serviceId, null, null, billSubType, operateTime, null, orderData.getDate(), null, orderData.getRemark());

        // 保存售后sub_file表
        saveServiceSubFile(subId, tombId, orderData, operateTime);

        Long paymentId = null;
        if (amount > 0 && orderData.getPaySerialNo() != null && !orderData.getPaySerialNo().isEmpty()) {
            String layerCode = queryForString("SELECT layer_code FROM t_sfi_tomb WHERE id = ?", tombId);
            paymentId = insertPayment(businessId, "1", orderData.getPaySerialNo(), layerCode, amount, operateTime);
            String payType = orderData.getPayType() != null ? orderData.getPayType().toString() : null;
            insertPaymentMethod(paymentId, amount, operateTime, payType);
        }

        List<String> itemIds = new ArrayList<>();
        List<String> itemNames = new ArrayList<>();
        List<ServiceOrderReserveData> listData = orderData.getServices();

        if (listData != null && !listData.isEmpty()) {
            for (ServiceOrderReserveData data : listData) {
                itemIds.add(data.getServiceId());
                itemNames.add(data.getServiceName());

                double price = new BigDecimal(data.getPrice()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).doubleValue();
                double qty = new BigDecimal(data.getQty()).doubleValue();

                // 保存业务服务表
                Long businessServiceId = insertBusinessService(businessId, serviceId, billType, subId, billSubType,
                        data.getServiceId(), data.getServiceName(), price, qty, operateTime);

                // 保存结算详情表
                if (paymentId != null) {
                    insertPaymentDetail(paymentId, businessServiceId, null, null, price * qty, operateTime);
                }
            }
        }

        if (!itemIds.isEmpty()) {
            jdbcTemplate.update("UPDATE t_sfi_service_sub SET detail_item_id_array = ?, detail_item_name_array = ? WHERE id = ?",
                    String.join(",", itemIds), String.join(",", itemNames), subId);
        }

        // 保存发票
        saveOrUpdatePaymentInvoice(businessId, queryForString("SELECT business_no FROM t_sfi_business WHERE id = ?", businessId),
                orderData, operateTime);

        // 保存工单信息
        saveAfterServiceInfoForWork(businessId, tombId, orderData, billType, billSubType, operateTime, serviceId);
    }

    private void saveAfterServiceInfoForUpdate(Long businessId, Long tombId, ServiceOrderData orderData,
                                               String billType, String billSubType, Date operateTime, long serviceId) {
        jdbcTemplate.update("UPDATE t_sfi_service SET modify_date = ?, modify_user = ? WHERE id = ?",
                operateTime, CREATE_USER_ID, serviceId);

        // 更新发票
        saveOrUpdatePaymentInvoice(businessId, queryForString("SELECT business_no FROM t_sfi_business WHERE id = ?", businessId),
                orderData, operateTime);
        // 更新工单信息
        saveAfterServiceInfoForWork(businessId, tombId, orderData, billType, billSubType, operateTime, serviceId);
    }

    private void saveServiceSubFile(long subId, Long tombId, ServiceOrderData orderData, Date operateTime) {
        if (orderData.getOrderImages() == null || orderData.getOrderImages().isEmpty()) {
            return;
        }
        String layerCode = queryForString("SELECT layer_code FROM t_sfi_tomb WHERE id = ?", tombId);
        for (com.scaffold.cemlink.client.model.OrderImageData imageData : orderData.getOrderImages()) {
            String filePath = "/interment/uploadFile/" + layerCode + "/";
            String fileName = imageData.getUrl().substring(imageData.getUrl().lastIndexOf("/") + 1);
            long fileSize = downloadFile(imageData.getUrl(), filePath, fileName);

            jdbcTemplate.update("INSERT INTO t_sfi_service_sub_flie (id, is_validate, create_date, create_user, "
                            + "service_sub_id, file_path, file_name, file_size, file_suffix) VALUES (?, '1', ?, ?, ?, ?, ?, ?, ?)",
                    nextId("t_sfi_service_sub_flie"), operateTime, CREATE_USER_ID, subId, filePath + fileName,
                    fileName, (double) fileSize, fileName.substring(fileName.lastIndexOf(".") + 1));
        }
    }

    private void saveAfterServiceInfoForWork(Long businessId, Long tombId, ServiceOrderData orderData,
                                             String billType, String billSubType, Date operateTime, long serviceId) {
        // 更新工单信息
        if (orderData.getCompleteTime() == null) {
            return;
        }

        jdbcTemplate.update("UPDATE t_sfi_service SET work_flag = '2' WHERE id = ?", serviceId);

        List<Long> subIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_service_sub WHERE service_id = ? AND is_validate = '1'", Long.class, serviceId);
        if (subIds.isEmpty()) {
            return;
        }
        long subId = subIds.get(0);
        jdbcTemplate.update("UPDATE t_sfi_service_sub SET work_flag = '1', work_status = ? WHERE id = ?",
                WORK_STATUS_ACCEPTED, subId);

        Long workId;
        List<Long> workIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_work WHERE bill_id = ? AND bill_type = ? AND bill_sub_id = ? AND bill_sub_type = ?",
                Long.class, serviceId, billType, subId, billSubType);
        if (workIds.isEmpty()) {
            workId = nextId("t_sfi_work");
            jdbcTemplate.update("INSERT INTO t_sfi_work (id, is_validate, create_date, create_user, register_user, "
                            + "register_user_id, business_id, bill_id, bill_type, bill_sub_id, bill_sub_type, work_status, "
                            + "work_date, check_date, finish_date, check_status, check_remark, operate_version) VALUES "
                            + "(?, '1', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '1', ?, 1)",
                    workId, operateTime, CREATE_USER_ID, CREATE_USER, CREATE_USER_ID, businessId, serviceId, billType,
                    subId, billSubType, WORK_STATUS_ACCEPTED, operateTime, orderData.getCompleteTime(),
                    orderData.getCompleteTime(), orderData.getCompleteResult());
        } else {
            workId = workIds.get(0);
            jdbcTemplate.update("UPDATE t_sfi_work SET work_status = ? WHERE id = ?", WORK_STATUS_ACCEPTED, workId);
        }

        boolean deleteFile = false;
        Long workCheckId;
        List<Long> workCheckIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_work_check WHERE work_id = ? AND is_validate = '1'", Long.class, workId);
        if (workCheckIds.isEmpty()) {
            workCheckId = nextId("t_sfi_work_check");
            jdbcTemplate.update("INSERT INTO t_sfi_work_check (id, is_validate, create_date, create_user, work_id, "
                            + "operate_date, operate_user, operate_user_id, remark, check_status, operate_version) VALUES "
                            + "(?, '1', ?, ?, ?, ?, ?, ?, ?, '1', 1)",
                    workCheckId, orderData.getCompleteTime(), CREATE_USER_ID, workId, orderData.getCompleteTime(),
                    orderData.getCompleteUser(), orderData.getCompleteUserId(), orderData.getCompleteResult());
        } else {
            workCheckId = workCheckIds.get(0);
            deleteFile = true;
        }

        if (orderData.getCompleteImages() != null && !orderData.getCompleteImages().isEmpty()) {
            if (deleteFile) {
                List<Long> fileIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_work_check_flie WHERE work_check_id = ? AND is_validate = '1'",
                        Long.class, workCheckId);
                for (Long fileId : fileIds) {
                    jdbcTemplate.update("UPDATE t_sfi_work_check_flie SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                            operateTime, CREATE_USER_ID, fileId);
                }
            }

            String layerCode = queryForString("SELECT layer_code FROM t_sfi_tomb WHERE id = ?", tombId);
            for (com.scaffold.cemlink.client.model.OrderImageData imageData : orderData.getCompleteImages()) {
                String filePath = "/interment/uploadFile/" + layerCode + "/";
                String fileName = imageData.getUrl().substring(imageData.getUrl().lastIndexOf("/") + 1);
                long fileSize = downloadFile(imageData.getUrl(), filePath, fileName);

                jdbcTemplate.update("INSERT INTO t_sfi_work_check_flie (id, is_validate, create_date, create_user, "
                                + "work_check_id, file_path, file_name, file_size, file_suffix) VALUES (?, '1', ?, ?, ?, ?, ?, ?, ?)",
                        nextId("t_sfi_work_check_flie"), operateTime, CREATE_USER_ID, workCheckId, filePath + fileName,
                        fileName, (double) fileSize, fileName.substring(fileName.lastIndexOf(".") + 1));
            }
        }
    }

    private void saveAfterServiceInfoForRetreat(Long businessId, String billType, String billSubType, Date operateTime,
                                                List<Long> serviceIds, ServiceOrderData orderData) {
        if (serviceIds.isEmpty()) {
            return;
        }
        long serviceId = serviceIds.get(0);
        jdbcTemplate.update("UPDATE t_sfi_service SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                operateTime, CREATE_USER_ID, serviceId);

        List<Long> subIds = jdbcTemplate.queryForList(
                "SELECT id, work_flag FROM t_sfi_service_sub WHERE service_id = ? AND is_validate = '1'", Long.class, serviceId);
        if (!subIds.isEmpty()) {
            long subId = subIds.get(0);
            jdbcTemplate.update("UPDATE t_sfi_service_sub SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                    operateTime, CREATE_USER_ID, subId);

            List<Long> businessServiceIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_business_service WHERE bill_id = ? AND bill_type = ? AND bill_sub_id = ? AND bill_sub_type = ?",
                    Long.class, serviceId, billType, subId, billSubType);
            for (Long businessServiceId : businessServiceIds) {
                jdbcTemplate.update("UPDATE t_sfi_business_service SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                        operateTime, CREATE_USER_ID, businessServiceId);
            }

            // 已经发单 删除工单
            List<Long> workIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_service_sub WHERE id = ? AND work_flag = '1'", Long.class, subId);
            if (!workIds.isEmpty() && !businessServiceIds.isEmpty()) {
                jdbcTemplate.update("UPDATE t_sfi_work SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = "
                        + "(SELECT id FROM t_sfi_work WHERE bill_id = ? AND bill_type = ? AND bill_sub_id = ? AND bill_sub_type = ?)",
                        operateTime, CREATE_USER_ID, serviceId, billType, subId, billSubType);
            }
        }

        saveOrUpdateRefundPayment(businessId, queryForString("SELECT business_no FROM t_sfi_business WHERE id = ?", businessId),
                orderData);
    }

    // ==================== 寄存续费 ====================

    private void saveDepositRenew(Long ashesBusinessId, ServiceOrderData orderData, Date operateTime, boolean refund) {
        String ashesId = String.valueOf(ashesBusinessId);
        if (refund) { // 退款
            List<Long> busDepositIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_ashes_bus_deposit WHERE business_id = ? AND order_id = ?",
                    Long.class, ashesId, orderData.getId());
            if (!busDepositIds.isEmpty()) {
                long busDepositId = busDepositIds.get(0);
                jdbcTemplate.update("UPDATE t_sfi_ashes_bus_deposit SET last_flag = '0', is_validate = '0', "
                        + "modify_date = ?, modify_user = ? WHERE id = ?", operateTime, CREATE_USER_ID, busDepositId);

                List<Long> busItemIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_ashes_bus_item WHERE business_id = ? AND bill_id = ? AND is_validate = '1'",
                        Long.class, ashesId, busDepositId);
                if (!busItemIds.isEmpty()) {
                    jdbcTemplate.update("UPDATE t_sfi_ashes_bus_item SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                            operateTime, CREATE_USER_ID, busItemIds.get(0));
                }

                List<Long> lastDepositIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_ashes_bus_deposit WHERE business_id = ? AND is_validate = '1' "
                                + "ORDER BY deposit_index DESC", Long.class, ashesId);
                if (!lastDepositIds.isEmpty()) {
                    jdbcTemplate.update("UPDATE t_sfi_ashes_bus_deposit SET last_flag = '1' WHERE id = ?", lastDepositIds.get(0));
                }

                List<Long> paymentIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_payment WHERE pay_no = ? AND is_validate = '1'",
                        Long.class, orderData.getPaySerialNo());
                if (!paymentIds.isEmpty()) {
                    long paymentId = paymentIds.get(0);
                    jdbcTemplate.update("UPDATE t_sfi_payment SET payment_type = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                            operateTime, CREATE_USER_ID, paymentId);

                    List<Long> invoiceIds = jdbcTemplate.queryForList(
                            "SELECT id FROM t_sfi_pay_invoice WHERE payment_id = ? AND is_validate = '1'",
                            Long.class, paymentId);
                    if (!invoiceIds.isEmpty()) {
                        jdbcTemplate.update("UPDATE t_sfi_pay_invoice SET payment_type = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                                operateTime, CREATE_USER_ID, invoiceIds.get(0));
                    }
                }

                jdbcTemplate.update("UPDATE t_sfi_ashes_business SET modify_date = ? WHERE id = ?", operateTime, ashesId);
            }
        } else {
            List<Long> busDepositIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_ashes_bus_deposit WHERE business_id = ? AND order_id = ?",
                    Long.class, ashesId, orderData.getId());

            if (busDepositIds.isEmpty()) {
                double amount = orderData.getAmount() / 100;  // 付款总金额
                int depositIndex = 1;
                String busGridId = null;
                String depositItemId = null;

                List<String> busGridIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_ashes_bus_grid WHERE business_id = ? AND is_validate = '1'",
                        String.class, ashesId);
                if (!busGridIds.isEmpty()) {
                    busGridId = busGridIds.get(0);
                }

                List<Map<String, Object>> busItems = jdbcTemplate.queryForList(
                        "SELECT id, item_id FROM t_sfi_ashes_bus_item WHERE business_id = ? AND bill_type = '0001' "
                                + "AND is_validate = '1' ORDER BY id DESC LIMIT 1", ashesId);
                if (!busItems.isEmpty() && busItems.get(0).get("item_id") != null) {
                    depositItemId = busItems.get(0).get("item_id").toString();
                }

                List<Long> lastIndexes = jdbcTemplate.queryForList(
                        "SELECT MAX(deposit_index) FROM t_sfi_ashes_bus_deposit WHERE business_id = ?",
                        Long.class, ashesId);
                if (lastIndexes.get(0) != null) {
                    depositIndex = lastIndexes.get(0).intValue() + 1;
                }

                List<Long> lastFlagIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_ashes_bus_deposit WHERE business_id = ? AND last_flag = '1' AND is_validate = '1'",
                        Long.class, ashesId);
                for (Long m : lastFlagIds) {
                    jdbcTemplate.update("UPDATE t_sfi_ashes_bus_deposit SET last_flag = '0' WHERE id = ?", m);
                }

                Long paymentId = insertPayment(ashesBusinessId, "3", orderData.getPaySerialNo(), null, amount, operateTime);

                // 保存寄存表
                long busDepositId = nextId("t_sfi_ashes_bus_deposit");
                jdbcTemplate.update("INSERT INTO t_sfi_ashes_bus_deposit (id, create_date, create_user, is_validate, "
                                + "business_id, bus_grid_id, deposit_year, deposit_fee, deposit_index, start_date, end_date, "
                                + "pay_status, pay_date, last_flag, remark, sale_office, order_id) VALUES "
                                + "(?, ?, 12618, '1', ?, ?, ?, ?, ?, ?, ?, '1', ?, '1', ?, '28', ?)",
                        busDepositId, operateTime, ashesId, busGridId, orderData.getQty(), amount, depositIndex,
                        orderData.getStartTime(), orderData.getEndTime(), orderData.getPayTime(), REMAEK, orderData.getId());

                // 保存寄存收费表
                long busItemId = nextId("t_sfi_ashes_bus_item");
                insertBusItem(busItemId, ashesBusinessId, busGridId, busDepositId, "0010", depositItemId, "续存费",
                        orderData.getQty(), amount, orderData.getStartTime(), orderData.getEndTime(), orderData.getPayTime());

                // 保存结算详情表
                insertPaymentDetail(paymentId, null, null, busItemId, amount, operateTime);

                String payType = orderData.getPayType() != null ? orderData.getPayType().toString() : null;
                insertPaymentMethod(paymentId, amount, operateTime, payType);

                jdbcTemplate.update("UPDATE t_sfi_ashes_business SET modify_date = ? WHERE id = ?", operateTime, ashesId);

                // 保存寄存发票
                saveOrUpdateDepositPaymentInvoice(ashesBusinessId, ashesId, queryForString(
                        "SELECT business_no FROM t_sfi_ashes_business WHERE id = ?", ashesId), orderData, operateTime);
            } else {
                // 更新发票
                saveOrUpdateDepositPaymentInvoice(ashesBusinessId, ashesId, queryForString(
                        "SELECT business_no FROM t_sfi_ashes_business WHERE id = ?", ashesId), orderData, operateTime);
            }
        }
    }

    // ==================== 寄存祭扫 ====================

    private void saveDepositSacrifice(Long ashesBusinessId, ServiceOrderData orderData, Date operateTime, boolean refund) {
        List<Long> serviceIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_ashes_bus_after_service WHERE order_id = ? AND is_validate = '1'",
                Long.class, orderData.getId());
        if (refund) { // 退单
            if (serviceIds.isEmpty()) {
                return;
            }
            jdbcTemplate.update("UPDATE t_sfi_ashes_bus_after_service SET is_validate = '0', modify_date = ?, modify_user = ? WHERE id = ?",
                    operateTime, CREATE_USER_ID, serviceIds.get(0));

            List<Long> paymentIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_payment WHERE pay_no = ? AND is_validate = '1'",
                    Long.class, orderData.getPaySerialNo());
            if (!paymentIds.isEmpty()) {
                long paymentId = paymentIds.get(0);
                // 保存退款记录
                long refundPaymentId = nextId("t_sfi_payment");
                jdbcTemplate.update("INSERT INTO t_sfi_payment (id, is_validate, create_date, create_user, business_id, "
                                + "pay_no, business_type, business_source, pay_date, pay_user, pay_user_id, pay_sum, discount_sum, "
                                + "fact_sum, payment_type, print_flag, refund_flag) "
                                + "SELECT ?, is_validate, ?, create_user, business_id, pay_no, business_type, business_source, "
                                + "?, ?, pay_user, pay_user_id, -pay_sum, discount_sum, -fact_sum, '2', print_flag, refund_flag "
                                + "FROM t_sfi_payment WHERE id = ?",
                        refundPaymentId, operateTime, operateTime, PAY_USER, paymentId);

                List<Long> paymentDetailIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_payment_detail WHERE payment_id = ? AND is_validate = '1'", Long.class, paymentId);
                for (Long detailId : paymentDetailIds) {
                    long refundDetailId = nextId("t_sfi_payment_detail");
                    jdbcTemplate.update("INSERT INTO t_sfi_payment_detail (id, is_validate, create_date, create_user, "
                                    + "payment_id, bus_service_id, retail_detail_id, deposit_item_id, fact_sum) "
                                    + "SELECT ?, is_validate, create_date, create_user, ?, bus_service_id, retail_detail_id, "
                                    + "deposit_item_id, -fact_sum FROM t_sfi_payment_detail WHERE id = ?",
                            refundDetailId, refundPaymentId, detailId);
                }

                List<Long> paymentMethodIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_payment_method WHERE payment_id = ? AND is_validate = '1'", Long.class, paymentId);
                for (Long methodId : paymentMethodIds) {
                    long refundMethodId = nextId("t_sfi_payment_method");
                    jdbcTemplate.update("INSERT INTO t_sfi_payment_method (id, is_validate, create_date, create_user, "
                                    + "payment_id, method_id, fact_sum) SELECT ?, is_validate, create_date, create_user, ?, "
                                    + "method_id, fact_sum FROM t_sfi_payment_method WHERE id = ?",
                            refundMethodId, refundPaymentId, methodId);
                }

                jdbcTemplate.update("UPDATE t_sfi_payment SET refund_flag = '1' WHERE id = ?", paymentId);

                if (orderData.getInvoices() != null && !orderData.getInvoices().isEmpty()) {
                    List<ServiceOrderInvoiceData> redInvices = filterInvoices(orderData.getInvoices(), "1");
                    if (!redInvices.isEmpty()) {
                        List<Long> invoiceIds = jdbcTemplate.queryForList(
                                "SELECT id FROM t_sfi_pay_invoice WHERE payment_id = ? AND is_validate = '1'",
                                Long.class, paymentId);
                        if (!invoiceIds.isEmpty()) {
                            long payInvoiceId = invoiceIds.get(0);
                            long redPayInvoiceId = nextId("t_sfi_pay_invoice");
                            jdbcTemplate.update("INSERT INTO t_sfi_pay_invoice (id, is_validate, create_date, create_user, "
                                            + "business_id, business_type, business_source, invoice_date, invoice_user, invoice_user_id, "
                                            + "invoice_sum, discount_sum, fact_sum, invoice_no, payment_type, invoice_unit, print_flag, "
                                            + "refund_flag, invoice_code, return_tomb_flag, is_tax, tax_type, payment_id) "
                                            + "SELECT ?, is_validate, ?, create_user, business_id, business_type, business_source, ?, "
                                            + "invoice_user, invoice_user_id, -invoice_sum, discount_sum, -fact_sum, ?, '2', invoice_unit, "
                                            + "print_flag, refund_flag, invoice_code, return_tomb_flag, is_tax, tax_type, ? "
                                            + "FROM t_sfi_pay_invoice WHERE id = ?",
                                    redPayInvoiceId, operateTime, operateTime,
                                    redInvices.get(0).getInvoiceCode() + redInvices.get(0).getInvoiceNo(),
                                    refundPaymentId, payInvoiceId);

                            List<Long> invoiceDetailIds = jdbcTemplate.queryForList(
                                    "SELECT id FROM t_sfi_pay_invoice_detail WHERE invoice_id = ? AND is_validate = '1'",
                                    Long.class, payInvoiceId);
                            for (Long detailId : invoiceDetailIds) {
                                long redDetailId = nextId("t_sfi_pay_invoice_detail");
                                jdbcTemplate.update("INSERT INTO t_sfi_pay_invoice_detail (id, is_validate, create_date, create_user, "
                                                + "invoice_id, bus_service_id, retail_detail_id, deposit_item_id, fact_sum) "
                                                + "SELECT ?, is_validate, create_date, create_user, ?, bus_service_id, retail_detail_id, "
                                                + "deposit_item_id, -fact_sum FROM t_sfi_pay_invoice_detail WHERE id = ?",
                                        redDetailId, redPayInvoiceId, detailId);
                            }

                            jdbcTemplate.update("UPDATE t_sfi_pay_invoice SET refund_flag = '1' WHERE id = ?", payInvoiceId);
                            jdbcTemplate.update("UPDATE t_sfi_payment SET invoice_no = ? WHERE id = ?",
                                    redInvices.get(0).getInvoiceCode() + redInvices.get(0).getInvoiceNo(), refundPaymentId);
                        }
                    }
                }
            }
        } else {
            if (serviceIds.isEmpty()) { // 新增
                double amount = orderData.getAmount() / 100;  // 付款总金额

                long afterServiceId = nextId("t_sfi_ashes_bus_after_service");
                jdbcTemplate.update("INSERT INTO t_sfi_ashes_bus_after_service (id, is_validate, create_date, create_user, "
                                + "register_date, register_user, register_user_id, finish_date, business_id, status, pay_status, "
                                + "remark, order_id, order_type) VALUES (?, '1', ?, ?, ?, ?, ?, ?, ?, '0', ?, ?, ?, ?)",
                        afterServiceId, operateTime, CREATE_USER_ID, operateTime, CREATE_USER, CREATE_USER_ID,
                        operateTime, ashesBusinessId, PAY_STATUS_FULL,
                        orderData.getRemark() != null && !orderData.getRemark().isEmpty()
                                ? orderData.getRemark() + REMAEK : REMAEK,
                        orderData.getId(), OrderType.寄存祭扫.getCode());

                String busGridId = null;
                List<String> busGridIds = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_ashes_bus_grid WHERE business_id = ? AND is_validate = '1'",
                        String.class, String.valueOf(ashesBusinessId));
                if (!busGridIds.isEmpty()) {
                    busGridId = busGridIds.get(0);
                }

                Long paymentId = null;
                if (amount > 0 && orderData.getPaySerialNo() != null && !orderData.getPaySerialNo().isEmpty()) {
                    paymentId = insertPayment(ashesBusinessId, "3", orderData.getPaySerialNo(), null, amount, operateTime);
                    String payType = orderData.getPayType() != null ? orderData.getPayType().toString() : null;
                    insertPaymentMethod(paymentId, amount, operateTime, payType);
                }

                List<ServiceOrderReserveData> listData = orderData.getServices();
                if (listData != null && !listData.isEmpty()) {
                    for (ServiceOrderReserveData data : listData) {
                        double price = new BigDecimal(data.getPrice()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).doubleValue();
                        double qty = new BigDecimal(data.getQty()).doubleValue();

                        // 保存业务服务表
                        long busItemId = nextId("t_sfi_ashes_bus_item");
                        insertBusItem(busItemId, ashesBusinessId, busGridId, afterServiceId, "0014",
                                data.getServiceId(), data.getServiceName(), null, price * qty, null, null,
                                orderData.getPayTime());

                        // 保存结算详情表
                        if (paymentId != null) {
                            insertPaymentDetail(paymentId, null, null, busItemId, price * qty, operateTime);
                        }
                    }
                }

                jdbcTemplate.update("UPDATE t_sfi_ashes_business SET modify_date = ? WHERE id = ?",
                        operateTime, String.valueOf(ashesBusinessId));

                // 保存寄存发票
                saveOrUpdateDepositPaymentInvoice(ashesBusinessId, String.valueOf(ashesBusinessId), queryForString(
                        "SELECT business_no FROM t_sfi_ashes_business WHERE id = ?", String.valueOf(ashesBusinessId)),
                        orderData, operateTime);
            } else { // 同步更新
                long serviceId = serviceIds.get(0);
                jdbcTemplate.update("UPDATE t_sfi_ashes_bus_after_service SET modify_date = ?, modify_user = ?, remark = ? WHERE id = ?",
                        operateTime, CREATE_USER_ID, orderData.getRemark(), serviceId);

                // 更新发票
                saveOrUpdateDepositPaymentInvoice(ashesBusinessId, String.valueOf(ashesBusinessId), queryForString(
                        "SELECT business_no FROM t_sfi_ashes_business WHERE id = ?", String.valueOf(ashesBusinessId)),
                        orderData, operateTime);

                // 更新工单信息
                if (orderData.getCompleteTime() != null) {
                    jdbcTemplate.update("UPDATE t_sfi_ashes_bus_after_service SET send_date = ?, check_date = ? WHERE id = ?",
                            operateTime, operateTime, serviceId);
                }
            }
        }
    }

    // ==================== 在线售墓（保留墓穴） ====================

    private void saveBookTomb(ServiceOrderData orderData, Date operateTime) {
        List<Long> tombs = jdbcTemplate.queryForList(
                "SELECT id, status_id, basic_price FROM t_sfi_tomb WHERE id = ?", Long.class, Long.parseLong(orderData.getTombId()));
        if (tombs.isEmpty()) {
            return;
        }
        Long statusId = queryForLong("SELECT status_id FROM t_sfi_tomb WHERE id = ?", tombs.get(0));
        if (statusId == null || !"2".equals(statusId.toString())) {
            return;
        }
        Double basicPrice = queryForDouble("SELECT basic_price FROM t_sfi_tomb WHERE id = ?", tombs.get(0));

        long bookingId = nextId("t_sfi_booking");
        jdbcTemplate.update("INSERT INTO t_sfi_booking (id, valid_flag, business_flag, base_sum, booking_no, book_day, "
                        + "apply_date, start_time, end_time, remark, is_validate, create_date, create_user, order_id, order_type) "
                        + "VALUES (?, '1', '0', ?, ?, 7, ?, ?, ?, ?, '1', ?, ?, ?, ?)",
                bookingId, basicPrice, orderData.getOrderNo(), operateTime, operateTime,
                addDay(operateTime, 7), orderData.getRemark(), operateTime, CREATE_USER_ID, orderData.getId(),
                orderData.getOrderType());

        // 保存保留墓穴表
        jdbcTemplate.update("INSERT INTO t_sfi_booking_tomb (id, booking_id, tomb_id, tomb_status_id, is_validate, "
                        + "create_date, create_user) VALUES (?, ?, ?, ?, '1', ?, ?)",
                nextId("t_sfi_booking_tomb"), bookingId, tombs.get(0), statusId.toString(), operateTime, CREATE_USER_ID);

        // 保存保留联系人表
        jdbcTemplate.update("INSERT INTO t_sfi_book_linkman (id, booking_id, name, mobile_no, is_validate, "
                        + "create_date, create_user) VALUES (?, ?, ?, ?, '1', ?, ?)",
                nextId("t_sfi_book_linkman"), bookingId, orderData.getName(), orderData.getPhone(), operateTime, CREATE_USER_ID);

        // 保存保留使用人表
        if (orderData.getOtherData() != null && !orderData.getOtherData().isEmpty()) {
            List<String> userNames = new ArrayList<>(orderData.getOtherData().size());
            for (Map<String, String> u : orderData.getOtherData()) {
                String deadName = u.containsKey("deadName") ? u.get("deadName") : null;
                String deadSex = u.containsKey("deadSex") ? u.get("deadSex") : null;

                jdbcTemplate.update("INSERT INTO t_sfi_book_user (id, booking_id, name, sex, is_validate, "
                                + "create_date, create_user) VALUES (?, ?, ?, ?, '1', ?, ?)",
                        nextId("t_sfi_book_user"), bookingId, deadName, "男".equals(deadSex) ? "1" : "2",
                        operateTime, CREATE_USER_ID);

                if (deadName != null && !deadName.isEmpty()) {
                    userNames.add(deadName);
                }
            }
            jdbcTemplate.update("UPDATE t_sfi_booking SET user_name = ? WHERE id = ?",
                    String.join(",", userNames), bookingId);
        }
    }

    // ==================== 寄存预约 ====================

    private void saveAshesDepositRegister(ServiceOrderData orderData, Date operateTime, boolean cancel) {
        if (cancel) {
            List<Long> customerOrderIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_ashes_customer_order WHERE order_id = ?", Long.class, orderData.getId());
            if (!customerOrderIds.isEmpty()) {
                jdbcTemplate.update("UPDATE t_sfi_ashes_customer_order SET status = '9', modify_date = ?, modify_user = ? WHERE id = ?",
                        operateTime, CREATE_USER_ID, customerOrderIds.get(0));
            }
        } else {
            Map<String, Object> extendData = orderData.getExtendDataObj();

            String name = null, mobileNo = null, certificateType = null, certificateNo = null;
            String addressDetail = null, addressProvince = null, addressCity = null, addressCounty = null;
            String userName = null, userSex = null, userCertificateType = null, userCertificateNo = null;

            if (extendData != null) {
                if (extendData.containsKey("linkman")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> linkman = (LinkedHashMap<String, String>) extendData.get("linkman");
                    name = linkman.get("directorName");
                    mobileNo = linkman.get("directorPhone");
                    certificateType = linkman.get("idType");
                    certificateNo = linkman.get("idNo");
                    addressDetail = linkman.get("dwellingDetail");
                    if (linkman.containsKey("dwelling")) {
                        String[] dwellings = linkman.get("dwelling").split("-");
                        if (dwellings.length == 3) {
                            addressProvince = addZero(dwellings[0], 6);
                            addressCity = addZero(dwellings[1], 6);
                            addressCounty = addZero(dwellings[2], 6);
                        }
                    }
                }
                if (extendData.containsKey("userList")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> userList = (List<Map<String, String>>) extendData.get("userList");
                    Map<String, String> user = userList.get(0);
                    userName = user.get("name");
                    userSex = user.get("sex");
                    userCertificateType = user.get("idType");
                    userCertificateNo = user.get("idNo");
                }
            }

            jdbcTemplate.update("INSERT INTO t_sfi_ashes_customer_order (id, name, mobile_no, certificate_type, "
                            + "certificate_no, address_province, address_city, address_county, address_detail, user_name, "
                            + "user_sex, user_certificate_type, user_certificate_no, status, remark, is_validate, create_date, "
                            + "create_user, order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '1', ?, '1', ?, ?, ?)",
                    nextId("t_sfi_ashes_customer_order"), name, mobileNo, certificateType, certificateNo,
                    addressProvince, addressCity, addressCounty, addressDetail, userName, userSex,
                    userCertificateType, userCertificateNo, orderData.getRemark(), operateTime, CREATE_USER_ID,
                    orderData.getId());
        }
    }

    // ==================== 退款结算（退单共用） ====================

    private void saveOrUpdateRefundPayment(Long businessId, String businessNo, ServiceOrderData orderData) {
        List<Long> paymentIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_payment WHERE pay_no = ? AND payment_type = '1' AND is_validate = '1'",
                Long.class, orderData.getPaySerialNo());
        if (paymentIds.isEmpty()) {
            return;
        }
        long paymentId = paymentIds.get(0);

        List<Long> refundIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_payment WHERE pay_no = ? AND payment_type = '2' AND is_validate = '1'",
                Long.class, orderData.getPaySerialNo());
        long refundPaymentId;
        if (refundIds.isEmpty()) {
            // 保存退款记录
            refundPaymentId = nextId("t_sfi_payment");
            jdbcTemplate.update("INSERT INTO t_sfi_payment (id, is_validate, create_date, create_user, business_id, "
                            + "pay_no, business_type, business_source, pay_date, pay_user, pay_user_id, pay_sum, discount_sum, "
                            + "fact_sum, payment_type, print_flag, refund_flag) "
                            + "SELECT ?, is_validate, ?, create_user, business_id, pay_no, business_type, business_source, "
                            + "?, ?, pay_user, pay_user_id, -pay_sum, discount_sum, -fact_sum, '2', print_flag, refund_flag "
                            + "FROM t_sfi_payment WHERE id = ?",
                    refundPaymentId, new Date(), new Date(), paymentId);

            List<Long> paymentDetailIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_payment_detail WHERE payment_id = ? AND is_validate = '1'", Long.class, paymentId);
            for (Long detailId : paymentDetailIds) {
                long refundDetailId = nextId("t_sfi_payment_detail");
                jdbcTemplate.update("INSERT INTO t_sfi_payment_detail (id, is_validate, create_date, create_user, "
                                + "payment_id, bus_service_id, retail_detail_id, deposit_item_id, fact_sum) "
                                + "SELECT ?, is_validate, create_date, create_user, ?, bus_service_id, retail_detail_id, "
                                + "deposit_item_id, -fact_sum FROM t_sfi_payment_detail WHERE id = ?",
                        refundDetailId, refundPaymentId, detailId);
            }

            List<Long> paymentMethodIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_payment_method WHERE payment_id = ? AND is_validate = '1'", Long.class, paymentId);
            for (Long methodId : paymentMethodIds) {
                long refundMethodId = nextId("t_sfi_payment_method");
                jdbcTemplate.update("INSERT INTO t_sfi_payment_method (id, is_validate, create_date, create_user, "
                                + "payment_id, method_id, fact_sum) SELECT ?, is_validate, create_date, create_user, ?, "
                                + "method_id, fact_sum FROM t_sfi_payment_method WHERE id = ?",
                        refundMethodId, refundPaymentId, methodId);
            }

            jdbcTemplate.update("UPDATE t_sfi_payment SET refund_flag = '1' WHERE id = ?", paymentId);
        } else {
            refundPaymentId = refundIds.get(0);
            jdbcTemplate.update("UPDATE t_sfi_payment SET modify_date = ?, modify_user = ? WHERE id = ?",
                    new Date(), CREATE_USER_ID, refundPaymentId);
        }

        saveOrUpdateRefundInvoice(businessId, businessNo, paymentId, refundPaymentId, orderData);
    }

    private void saveOrUpdateRefundInvoice(Long businessId, String businessNo, long paymentId, long refundPaymentId,
                                           ServiceOrderData orderData) {
        if (orderData.getInvoices() == null || orderData.getInvoices().isEmpty()) {
            return;
        }
        List<ServiceOrderInvoiceData> redInvices = filterInvoices(orderData.getInvoices(), "1");
        if (redInvices.isEmpty()) {
            return;
        }
        ServiceOrderInvoiceData redInvoiceData = redInvices.get(0);

        List<Long> invoiceIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_pay_invoice WHERE payment_id = ? AND is_validate = '1'", Long.class, paymentId);
        if (invoiceIds.isEmpty()) {
            return;
        }
        long payInvoiceId = invoiceIds.get(0);

        List<Long> redIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_pay_invoice WHERE payment_id = ? AND is_validate = '1'", Long.class, refundPaymentId);
        long redPayInvoiceId;
        if (redIds.isEmpty()) {
            redPayInvoiceId = nextId("t_sfi_pay_invoice");
            jdbcTemplate.update("INSERT INTO t_sfi_pay_invoice (id, is_validate, create_date, create_user, business_id, "
                            + "business_type, business_source, invoice_date, invoice_user, invoice_user_id, invoice_sum, "
                            + "discount_sum, fact_sum, invoice_no, payment_type, invoice_unit, print_flag, refund_flag, "
                            + "invoice_code, return_tomb_flag, is_tax, tax_type, payment_id) "
                            + "SELECT ?, is_validate, create_date, create_user, business_id, business_type, business_source, ?, "
                            + "invoice_user, invoice_user_id, -invoice_sum, discount_sum, -fact_sum, ?, '2', invoice_unit, "
                            + "print_flag, refund_flag, invoice_code, return_tomb_flag, is_tax, tax_type, ? "
                            + "FROM t_sfi_pay_invoice WHERE id = ?",
                    redPayInvoiceId, redInvoiceData.getInvoiceTime(),
                    nullToEmpty(redInvoiceData.getInvoiceCode()) + nullToEmpty(redInvoiceData.getInvoiceNo()),
                    refundPaymentId, payInvoiceId);

            List<Long> invoiceDetailIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_pay_invoice_detail WHERE invoice_id = ? AND is_validate = '1'",
                    Long.class, payInvoiceId);
            for (Long detailId : invoiceDetailIds) {
                long redDetailId = nextId("t_sfi_pay_invoice_detail");
                jdbcTemplate.update("INSERT INTO t_sfi_pay_invoice_detail (id, is_validate, create_date, create_user, "
                                + "invoice_id, bus_service_id, retail_detail_id, deposit_item_id, fact_sum) "
                                + "SELECT ?, is_validate, create_date, create_user, ?, bus_service_id, retail_detail_id, "
                                + "deposit_item_id, -fact_sum FROM t_sfi_pay_invoice_detail WHERE id = ?",
                        redDetailId, redPayInvoiceId, detailId);
            }

            jdbcTemplate.update("UPDATE t_sfi_pay_invoice SET refund_flag = '1', modify_date = ? WHERE id = ?",
                    new Date(), payInvoiceId);
            jdbcTemplate.update("UPDATE t_sfi_payment SET invoice_no = ? WHERE id = ?",
                    nullToEmpty(redInvoiceData.getInvoiceCode()) + nullToEmpty(redInvoiceData.getInvoiceNo()), refundPaymentId);
        } else {
            redPayInvoiceId = redIds.get(0);
        }

        jdbcTemplate.update("UPDATE t_sfi_pay_invoice SET refund_flag = '1' WHERE id = ?", payInvoiceId);
        jdbcTemplate.update("UPDATE t_sfi_payment SET invoice_no = ? WHERE id = ?",
                nullToEmpty(redInvoiceData.getInvoiceCode()) + nullToEmpty(redInvoiceData.getInvoiceNo()), refundPaymentId);

        updatePaymentInvoiceTax(businessId, businessNo, payInvoiceId, redPayInvoiceId, orderData);
    }

    // ==================== 发票（蓝票共用） ====================

    private void saveOrUpdatePaymentInvoice(Long businessId, String businessNo, ServiceOrderData orderData, Date operateTime) {
        if (orderData.getPaySerialNo() == null || orderData.getPaySerialNo().isEmpty()
                || orderData.getInvoices() == null || orderData.getInvoices().isEmpty()) {
            return;
        }
        List<Long> paymentIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_payment WHERE pay_no = ? AND is_validate = '1'", Long.class, orderData.getPaySerialNo());
        if (paymentIds.isEmpty()) {
            return;
        }
        long paymentId = paymentIds.get(0);

        List<ServiceOrderInvoiceData> blueInvices = filterInvoices(orderData.getInvoices(), "0");
        if (blueInvices.isEmpty()) {
            return;
        }
        ServiceOrderInvoiceData invoiceData = blueInvices.get(0);
        List<Long> invoiceIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_pay_invoice WHERE payment_id = ? AND is_validate = '1'", Long.class, paymentId);

        long payInvoiceId;
        if (invoiceIds.isEmpty()) {
            List<Map<String, Object>> paymentDetails = jdbcTemplate.queryForList(
                    "SELECT id, bus_service_id, fact_sum FROM t_sfi_payment_detail WHERE payment_id = ? AND is_validate = '1'",
                    paymentId);

            payInvoiceId = insertPayInvoice(businessId, paymentId, "1", invoiceData, operateTime);
            for (Map<String, Object> detail : paymentDetails) {
                Long busServiceId = detail.get("bus_service_id") == null ? null : ((Number) detail.get("bus_service_id")).longValue();
                Double factSum = detail.get("fact_sum") == null ? null : ((Number) detail.get("fact_sum")).doubleValue();
                insertPayInvoiceDetail(payInvoiceId, busServiceId, null, null, factSum, operateTime);

                if (busServiceId != null) {
                    jdbcTemplate.update("UPDATE t_sfi_business_service SET invoice_status = '1', invoice_date = ? WHERE id = ?",
                            operateTime, busServiceId);
                }
            }

            jdbcTemplate.update("UPDATE t_sfi_payment SET invoice_no = ? WHERE id = ?", invoiceData.getInvoiceNo(), paymentId);
        } else {
            payInvoiceId = invoiceIds.get(0);
        }

        updatePaymentInvoiceTax(businessId, businessNo, payInvoiceId, null, orderData);
    }

    private void saveOrUpdateDepositPaymentInvoice(Long businessId, String businessNo, ServiceOrderData orderData, Date operateTime) {
        saveOrUpdateDepositPaymentInvoice(businessId, String.valueOf(businessId), businessNo, orderData, operateTime);
    }

    private void saveOrUpdateDepositPaymentInvoice(Long businessId, String businessIdStr, String businessNo,
                                                   ServiceOrderData orderData, Date operateTime) {
        if (orderData.getPaySerialNo() == null || orderData.getPaySerialNo().isEmpty()
                || orderData.getInvoices() == null || orderData.getInvoices().isEmpty()) {
            return;
        }
        List<Long> paymentIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_payment WHERE pay_no = ? AND payment_type = '1' AND is_validate = '1'",
                Long.class, orderData.getPaySerialNo());
        if (paymentIds.isEmpty()) {
            return;
        }
        long paymentId = paymentIds.get(0);

        List<ServiceOrderInvoiceData> blueInvices = filterInvoices(orderData.getInvoices(), "0");
        if (blueInvices.isEmpty()) {
            return;
        }
        ServiceOrderInvoiceData invoiceData = blueInvices.get(0);
        List<Long> invoiceIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_pay_invoice WHERE payment_id = ? AND is_validate = '1'", Long.class, paymentId);

        long payInvoiceId;
        if (invoiceIds.isEmpty()) {
            List<Map<String, Object>> paymentDetails = jdbcTemplate.queryForList(
                    "SELECT id, deposit_item_id, fact_sum FROM t_sfi_payment_detail WHERE payment_id = ? AND is_validate = '1'",
                    paymentId);

            payInvoiceId = insertPayInvoice(businessId, paymentId, "3", invoiceData, operateTime);
            for (Map<String, Object> detail : paymentDetails) {
                Long depositItemId = detail.get("deposit_item_id") == null ? null : ((Number) detail.get("deposit_item_id")).longValue();
                Double factSum = detail.get("fact_sum") == null ? null : ((Number) detail.get("fact_sum")).doubleValue();
                insertPayInvoiceDetail(payInvoiceId, null, null, depositItemId, factSum, operateTime);

                if (depositItemId != null) {
                    jdbcTemplate.update("UPDATE t_sfi_ashes_bus_item SET invoice_status = '1', invoice_date = ? WHERE id = ?",
                            operateTime, depositItemId);
                }
            }

            jdbcTemplate.update("UPDATE t_sfi_payment SET invoice_no = ? WHERE id = ?", invoiceData.getInvoiceNo(), paymentId);
        } else {
            payInvoiceId = invoiceIds.get(0);
        }

        updatePaymentInvoiceTax(businessId, businessNo, payInvoiceId, null, orderData);
    }

    private void saveOrUpdateRetailPaymentInvoice(Long businessId, long retailId, String retailNo,
                                                  ServiceOrderData orderData, Date operateTime) {
        if (orderData.getPaySerialNo() == null || orderData.getPaySerialNo().isEmpty()
                || orderData.getInvoices() == null || orderData.getInvoices().isEmpty()) {
            return;
        }
        List<Long> paymentIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_payment WHERE pay_no = ? AND is_validate = '1'", Long.class, orderData.getPaySerialNo());
        if (paymentIds.isEmpty()) {
            return;
        }
        long paymentId = paymentIds.get(0);

        List<ServiceOrderInvoiceData> blueInvices = filterInvoices(orderData.getInvoices(), "0");
        if (blueInvices.isEmpty()) {
            return;
        }
        ServiceOrderInvoiceData invoiceData = blueInvices.get(0);
        List<Long> invoiceIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_pay_invoice WHERE payment_id = ? AND is_validate = '1'", Long.class, paymentId);

        long payInvoiceId;
        if (invoiceIds.isEmpty()) {
            List<Map<String, Object>> paymentDetails = jdbcTemplate.queryForList(
                    "SELECT id, bus_service_id, fact_sum FROM t_sfi_payment_detail WHERE payment_id = ? AND is_validate = '1'",
                    paymentId);

            payInvoiceId = insertPayInvoice(retailId, paymentId, "2", invoiceData, operateTime);
            for (Map<String, Object> detail : paymentDetails) {
                Long busServiceId = detail.get("bus_service_id") == null ? null : ((Number) detail.get("bus_service_id")).longValue();
                Double factSum = detail.get("fact_sum") == null ? null : ((Number) detail.get("fact_sum")).doubleValue();
                insertPayInvoiceDetail(payInvoiceId, busServiceId, null, null, factSum, operateTime);
            }

            jdbcTemplate.update("UPDATE t_sfi_payment SET invoice_no = ? WHERE id = ?", invoiceData.getInvoiceNo(), paymentId);
        } else {
            payInvoiceId = invoiceIds.get(0);
        }

        updatePaymentInvoiceTax(retailId, retailNo, payInvoiceId, null, orderData);
    }

    private void updatePaymentInvoiceTax(Long businessId, String businessNo, long payInvoiceId, Long redPayInvoiceId,
                                         ServiceOrderData orderData) {
        if (orderData.getInvoices() == null || orderData.getInvoices().isEmpty()) {
            return;
        }
        List<ServiceOrderInvoiceData> blueInvices = filterInvoices(orderData.getInvoices(), "0");
        List<ServiceOrderInvoiceData> redInvices = filterInvoices(orderData.getInvoices(), "1");
        if (blueInvices.isEmpty()) {
            return;
        }

        ServiceOrderInvoiceData blueInvoiceData = blueInvices.get(0);

        BigDecimal totalAmountYuan = new BigDecimal(blueInvoiceData.getTotalAmount()).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal totalTaxYuan = new BigDecimal(blueInvoiceData.getTotalTax()).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);

        Long taxId = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_payment_invoice_tax WHERE payment_id = ? AND is_validate = '1'",
                Long.class, payInvoiceId).stream().findFirst().orElse(null);
        if (taxId == null) {
            taxId = nextId("t_sfi_payment_invoice_tax");
            jdbcTemplate.update("INSERT INTO t_sfi_payment_invoice_tax (id, is_validate, create_date, create_user, "
                            + "business_id, business_no, business_type, payment_id) "
                            + "SELECT ?, is_validate, create_date, create_user, ?, business_no, business_type, ? "
                            + "FROM t_sfi_pay_invoice WHERE id = ?",
                    taxId, businessId, payInvoiceId, payInvoiceId);
        }
        jdbcTemplate.update("UPDATE t_sfi_payment_invoice_tax SET total_amount = ?, total_tax = ?, serial_no = ?, "
                        + "invoice_time = ?, invoice_code = ?, invoice_no = ?, pdf_url = ?, status = '3' WHERE id = ?",
                totalAmountYuan.doubleValue(), totalTaxYuan.doubleValue(), blueInvoiceData.getSerialNo(),
                blueInvoiceData.getInvoiceTime(), blueInvoiceData.getInvoiceCode(),
                nullToEmpty(blueInvoiceData.getInvoiceCode()) + nullToEmpty(blueInvoiceData.getInvoiceNo()),
                blueInvoiceData.getPdfUrl(), taxId);

        if (redInvices.isEmpty() || redPayInvoiceId == null) {
            return;
        }
        ServiceOrderInvoiceData redInvoiceData = redInvices.get(0);

        Long redTaxId = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_payment_invoice_tax WHERE payment_id = ? AND is_validate = '1'",
                Long.class, redPayInvoiceId).stream().findFirst().orElse(null);
        if (redTaxId == null) {
            redTaxId = nextId("t_sfi_payment_invoice_tax");
            jdbcTemplate.update("INSERT INTO t_sfi_payment_invoice_tax (id, is_validate, create_date, create_user, "
                            + "business_id, business_no, business_type, payment_id) "
                            + "SELECT ?, is_validate, create_date, create_user, ?, business_no, business_type, ? "
                            + "FROM t_sfi_pay_invoice WHERE id = ?",
                    redTaxId, businessId, redPayInvoiceId, redPayInvoiceId);
        }
        jdbcTemplate.update("UPDATE t_sfi_payment_invoice_tax SET total_amount = ?, total_tax = ?, serial_no = ?, "
                        + "invoice_time = ?, invoice_code = ?, invoice_no = ?, pdf_url = ?, status = '3' WHERE id = ?",
                -totalAmountYuan.doubleValue(), -totalTaxYuan.doubleValue(), redInvoiceData.getSerialNo(),
                redInvoiceData.getInvoiceTime(), redInvoiceData.getInvoiceCode(),
                nullToEmpty(redInvoiceData.getInvoiceCode()) + nullToEmpty(redInvoiceData.getInvoiceNo()),
                redInvoiceData.getPdfUrl(), redTaxId);
    }

    // ==================== convert 助手（原 BaseOrderDataCollectionService 平移） ====================

    private long insertService(Long businessId, ServiceOrderData orderData, Date operateTime) {
        long serviceId = nextId("t_sfi_service");
        jdbcTemplate.update("INSERT INTO t_sfi_service (id, is_validate, create_date, create_user, business_id, "
                        + "pay_status, work_flag, apply_date, register_user_id, register_user, remark, order_id, "
                        + "service_no, order_type) VALUES (?, '1', ?, ?, ?, ?, '0', ?, ?, ?, ?, ?, ?, ?)",
                serviceId, operateTime, CREATE_USER_ID, businessId, PAY_STATUS_FULL, operateTime,
                CREATE_USER_ID, CREATE_USER,
                orderData.getRemark() != null && !orderData.getRemark().isEmpty()
                        ? orderData.getRemark() + REMAEK : REMAEK,
                orderData.getId(), orderData.getOrderNo(), orderData.getOrderType());
        return serviceId;
    }

    private void insertServiceLinkman(long serviceId, Long linkmanId, Date operateTime) {
        jdbcTemplate.update("INSERT INTO t_sfi_service_linkman (id, is_validate, create_user, create_date, service_id, "
                        + "name, sex, address_province, address_city, address_county, address_detail, address_full, "
                        + "phone_no, mobile_no) "
                        + "SELECT ?, '1', ?, ?, ?, name, sex, address_province, address_city, address_county, "
                        + "address_detail, address_full, phone_no, mobile_no FROM t_sfi_bus_linkman WHERE id = ?",
                nextId("t_sfi_service_linkman"), CREATE_USER_ID, operateTime, serviceId, linkmanId);
    }

    private long insertServiceSub(long serviceId, String itemId, String itemName, String category,
                                  Date operateTime, Date startTime, Date endTime, Integer serviceTime, String remark) {
        long subId = nextId("t_sfi_service_sub");
        jdbcTemplate.update("INSERT INTO t_sfi_service_sub (id, is_validate, create_date, create_user, source_type, "
                        + "register_user, register_user_id, service_id, category, pay_status, work_status, send_flag, "
                        + "work_flag, message_flag, apply_date, finish_date, start_time, end_time, service_time, "
                        + "detail_item_id_array, detail_item_name_array, remark) VALUES "
                        + "(?, '1', ?, ?, '2', ?, ?, ?, ?, ?, '0', '0', '0', '0', ?, ?, ?, ?, ?, ?, ?, ?)",
                subId, operateTime, CREATE_USER_ID, CREATE_USER, CREATE_USER_ID, serviceId, category, PAY_STATUS_FULL,
                operateTime, operateTime, startTime, endTime, serviceTime, itemId, itemName,
                remark != null && !remark.isEmpty() ? remark + REMAEK : REMAEK);
        return subId;
    }

    private Long insertPayment(Long businessId, String businessType, String payNo, String layerCode,
                               Double amount, Date operateTime) {
        long paymentId = nextId("t_sfi_payment");
        jdbcTemplate.update("INSERT INTO t_sfi_payment (id, is_validate, create_date, create_user, business_id, "
                        + "pay_no, business_type, business_source, layer_code, pay_date, pay_user, pay_user_id, pay_sum, "
                        + "fact_sum, discount_sum, print_flag, refund_flag, payment_type) VALUES "
                        + "(?, '1', ?, ?, ?, ?, ?, '2', ?, ?, ?, ?, ?, ?, 0, '0', '0', '1')",
                paymentId, operateTime, CREATE_USER_ID, businessId, payNo, businessType, layerCode, operateTime,
                PAY_USER, CREATE_USER_ID, amount, amount);
        return paymentId;
    }

    private void insertPaymentDetail(Long paymentId, Long busServiceId, Long retailDetailId, Long depositItemId,
                                     Double factSum, Date operateTime) {
        jdbcTemplate.update("INSERT INTO t_sfi_payment_detail (id, is_validate, create_date, create_user, "
                        + "payment_id, bus_service_id, retail_detail_id, deposit_item_id, fact_sum) VALUES "
                        + "(?, '1', ?, ?, ?, ?, ?, ?, ?)",
                nextId("t_sfi_payment_detail"), operateTime, CREATE_USER_ID, paymentId, busServiceId,
                retailDetailId, depositItemId, factSum);
    }

    private void insertPaymentMethod(Long paymentId, Double factSum, Date operateTime, String payType) {
        Integer methodId = null;
        if (payType != null && !payType.isEmpty()) {
            List<String> names = payTypeNames(payType);
            if (!names.isEmpty()) {
                List<Long> ids = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_code_method WHERE name = ?", Long.class, names.get(0));
                if (!ids.isEmpty()) {
                    methodId = ids.get(0).intValue();
                }
            }
        }
        jdbcTemplate.update("INSERT INTO t_sfi_payment_method (id, is_validate, create_date, create_user, "
                        + "payment_id, method_id, fact_sum) VALUES (?, '1', ?, ?, ?, ?, ?)",
                nextId("t_sfi_payment_method"), operateTime, CREATE_USER_ID, paymentId, methodId, factSum);
    }

    /** 原契约 PayType 名称映射（原程序 PaymentMethodDao.findCodeMethodId(PayType.getNameByCode(payType))） */
    private List<String> payTypeNames(String payType) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("1", "公众号付款");
        map.put("2", "支付宝");
        map.put("98", "福寿云平台");
        List<String> result = new ArrayList<>();
        if (map.containsKey(payType)) {
            result.add(map.get(payType));
        }
        return result;
    }

    private long insertPayInvoice(Long businessId, Long paymentId, String businessType,
                                  ServiceOrderInvoiceData invoiceData, Date operateTime) {
        BigDecimal totalAmountYuan = new BigDecimal(invoiceData.getTotalAmount()).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        long payInvoiceId = nextId("t_sfi_pay_invoice");
        jdbcTemplate.update("INSERT INTO t_sfi_pay_invoice (id, is_validate, create_date, create_user, payment_id, "
                        + "business_id, business_type, business_source, invoice_unit, invoice_date, invoice_user, "
                        + "invoice_user_id, invoice_sum, fact_sum, discount_sum, print_flag, refund_flag, return_tomb_flag, "
                        + "payment_type, invoice_no, invoice_code, is_tax, tax_type) VALUES "
                        + "(?, '1', ?, 9999, ?, ?, ?, '2', ?, ?, ?, ?, ?, ?, 0, '1', '0', '0', '1', ?, ?, '1', '026')",
                payInvoiceId, operateTime, paymentId, businessId, businessType, invoiceData.getInvoiceTitle(),
                operateTime, invoiceData.getInvoiceUser() != null && !invoiceData.getInvoiceUser().isEmpty()
                        ? invoiceData.getInvoiceUser() : PAY_USER,
                CREATE_USER_ID, totalAmountYuan.doubleValue(), totalAmountYuan.doubleValue(),
                nullToEmpty(invoiceData.getInvoiceCode()) + nullToEmpty(invoiceData.getInvoiceNo()),
                invoiceData.getInvoiceCode());
        return payInvoiceId;
    }

    private void insertPayInvoiceDetail(Long payInvoiceId, Long busServiceId, Long retailDetailId, Long depositItemId,
                                        Double factSum, Date operateTime) {
        jdbcTemplate.update("INSERT INTO t_sfi_pay_invoice_detail (id, is_validate, create_date, create_user, "
                        + "invoice_id, bus_service_id, retail_detail_id, deposit_item_id, fact_sum) VALUES "
                        + "(?, '1', ?, ?, ?, ?, ?, ?, ?)",
                nextId("t_sfi_pay_invoice_detail"), operateTime, CREATE_USER_ID, payInvoiceId, busServiceId,
                retailDetailId, depositItemId, factSum);
    }

    private Long insertBusinessService(Long businessId, Long billId, String billType, Long billSubId, String billSubType,
                                       String serviceId, String name, Double servicePrice, Double qty, Date operateTime) {
        long businessServiceId = nextId("t_sfi_business_service");
        jdbcTemplate.update("INSERT INTO t_sfi_business_service (id, is_validate, create_date, create_user, business_id, "
                        + "bill_id, bill_sub_id, bill_type, bill_sub_type, service_id, name, qty, price, `sum`, pay_status, "
                        + "pay_date, invoice_status) VALUES (?, '1', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')",
                businessServiceId, operateTime, CREATE_USER_ID, businessId, billId, billSubId, billType,
                billSubType, serviceId, name, qty, servicePrice, servicePrice * qty, PAY_EASY_SETTLED, operateTime);
        return businessServiceId;
    }

    private void insertBusItem(long busItemId, Long businessId, String busGridId, Long billId, String billType,
                               String itemId, String name, Integer year, double fee, Date startDate, Date endDate,
                               Date operateTime) {
        jdbcTemplate.update("INSERT INTO t_sfi_ashes_bus_item (id, is_validate, create_date, create_user, business_id, "
                        + "bus_grid_id, bill_id, bill_type, item_id, name, `year`, qty, price, `sum`, start_date, end_date, "
                        + "sale_office, pay_status, pay_date, invoice_status) VALUES "
                        + "(?, '1', ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, '28', ?, ?, '0')",
                busItemId, operateTime, CREATE_USER_ID, String.valueOf(businessId), busGridId, billId, billType,
                itemId, name, year, fee, fee, startDate, endDate, PAY_EASY_SETTLED, operateTime);
    }

    // ==================== 通用助手 ====================

    private long nextId(String table) {
        Long max = jdbcTemplate.queryForObject("SELECT IFNULL(MAX(id), 0) FROM " + table, Long.class);
        return max + 1;
    }

    private String queryForString(String sql, Object... args) {
        List<String> values = jdbcTemplate.queryForList(sql, String.class, args);
        return values.isEmpty() ? null : values.get(0);
    }

    private Long queryForLong(String sql, Object... args) {
        List<Long> values = jdbcTemplate.queryForList(sql, Long.class, args);
        return values.isEmpty() ? null : values.get(0);
    }

    private Double queryForDouble(String sql, Object... args) {
        List<Double> values = jdbcTemplate.queryForList(sql, Double.class, args);
        return values.isEmpty() ? null : values.get(0);
    }

    private List<ServiceOrderInvoiceData> filterInvoices(List<ServiceOrderInvoiceData> invoices, String type) {
        List<ServiceOrderInvoiceData> result = new ArrayList<>();
        for (ServiceOrderInvoiceData invoice : invoices) {
            if (type.equals(invoice.getInvoiceType())) {
                result.add(invoice);
            }
        }
        return result;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Date addDay(Date date, int days) {
        return new Date(date.getTime() + days * 24L * 3600L * 1000L);
    }

    private String addZero(String str, int desiredLength) {
        StringBuilder sb = new StringBuilder(str == null ? "" : str);
        while (sb.length() < desiredLength) {
            sb.append('0');
        }
        return sb.toString();
    }

    /** 原程序 HttpUtil.downloadFile 平移（脚手架 http 封装，失败记录 0 字节） */
    private long downloadFile(String url, String filePath, String fileName) {
        try {
            TransportResponse response = scaffoldHttpClient.get(url);
            if (!response.isSuccessful()) {
                log.warn("[cemlink] 图片下载失败 url={} status={}", url, response.getStatus());
                return 0L;
            }
            Path dir = Paths.get(uploadPath, filePath);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.write(target, response.getBody());
            File file = target.toFile();
            return file.exists() ? file.length() : 0L;
        } catch (Exception e) {
            log.warn("[cemlink] 图片下载异常 url={} msg={}", url, e.getMessage());
            return 0L;
        }
    }
}
