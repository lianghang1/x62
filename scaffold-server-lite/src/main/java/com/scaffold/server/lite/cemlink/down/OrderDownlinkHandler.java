package com.scaffold.server.lite.cemlink.down;

import com.fasterxml.jackson.core.type.TypeReference;
import com.linkora.client.LinkoraReceivedMessage;
import com.linkora.client.MessageListener;
import com.scaffold.common.util.JsonUtils;
import com.scaffold.server.lite.cemlink.client.CemlinkTopics;
import com.scaffold.server.lite.cemlink.client.MallLinkoraClient;
import com.scaffold.server.lite.cemlink.client.model.ServiceOrderData;
import com.scaffold.server.lite.cemlink.client.model.ServiceOrderReserveData;
import com.scaffold.server.lite.cemlink.client.model.ServiceOrderUpdateData;
import com.scaffold.server.lite.cemlink.contract.OrderType;
import com.scaffold.starter.http.ScaffoldHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import com.scaffold.starter.data.jpa.datasource.TargetDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 售后订单下行回写：server 下发待同步订单 → 按类型写墓园业务表 → 回传同步状态
 *
 * @author lianghang
 */
@TargetDataSource("park")
public class OrderDownlinkHandler implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(OrderDownlinkHandler.class);

    private static final String COLLECTION_SUCCESS = "1";
    private static final String COLLECTION_ERROR = "0";

    private final JdbcTemplate jdbcTemplate;
    private final MallLinkoraClient mallLinkoraClient;
    private final OrderDownlinkWriter writer;

    public OrderDownlinkHandler(@Qualifier("cemlinkParkJdbc") JdbcTemplate jdbcTemplate,
                                MallLinkoraClient mallLinkoraClient,
                                ScaffoldHttpClient scaffoldHttpClient,
                                String uploadPath) {
        this.jdbcTemplate = jdbcTemplate;
        this.mallLinkoraClient = mallLinkoraClient;
        this.writer = new OrderDownlinkWriter(jdbcTemplate, scaffoldHttpClient, uploadPath);
    }

    @Override
    public void onMessage(LinkoraReceivedMessage message) {
        if (!CemlinkTopics.DOWNLINK_ORDER.equals(message.getMessageType())) {
            return;
        }
        try {
            List<ServiceOrderData> items = JsonUtils.fromJson(new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8), new TypeReference<List<ServiceOrderData>>() {
            });
            handle(items);
        } catch (Exception e) {
            throw new IllegalStateException("售后订单下行数据解析失败", e);
        }
    }

    /** 原程序 saveServiceInfo 逻辑平移：单条失败标记采集错误，成功列表回传 */
    public void handle(List<ServiceOrderData> orderDataList) {
        if (orderDataList == null || orderDataList.isEmpty()) {
            return;
        }
        Date operateTime = new Date();
        List<ServiceOrderUpdateData> updateOrders = new ArrayList<>(orderDataList.size());
        for (ServiceOrderData orderData : orderDataList) {
            List<Long> orderServiceIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sfi_bus_collection_order_service WHERE order_no = ?",
                    Long.class, orderData.getOrderNo());
            long orderServiceId = orderServiceIds.isEmpty()
                    ? nextId("t_sfi_bus_collection_order_service") : orderServiceIds.get(0);
            boolean exists = !orderServiceIds.isEmpty();

            try {
                Long businessId = null;
                Long tombId = null;
                Long linkmanId = null;
                Long ashesBusinessId = null;

                if (isNumeric(orderData.getTombId())) { // 公墓服务订单
                    if (!OrderType.预约看墓.getCode().equals(orderData.getOrderType())
                            && !OrderType.在线售墓.getCode().equals(orderData.getOrderType())) {
                        businessId = Long.parseLong(orderData.getTombId());
                        Long businessTombId = jdbcTemplate.queryForList(
                                "SELECT tomb_id FROM t_sfi_business WHERE id = ?", Long.class, businessId)
                                .stream().findFirst().orElse(null);
                        if (businessTombId != null) {
                            tombId = businessTombId;
                            List<Long> linkmanIds = jdbcTemplate.queryForList(
                                    "SELECT id FROM t_sfi_bus_linkman WHERE business_id = ?",
                                    Long.class, businessId);
                            if (!linkmanIds.isEmpty()) {
                                linkmanId = linkmanIds.get(0);
                            }
                        }
                    }
                } else if (isNumeric(orderData.getDepositId())) { // 寄存服务订单
                    ashesBusinessId = Long.parseLong(orderData.getDepositId());
                }

                writer.saveServiceInfo(orderData, operateTime, businessId, tombId, linkmanId, ashesBusinessId);

                Long busId = null;
                if (isNumeric(orderData.getTombId())) {
                    busId = Long.parseLong(orderData.getTombId());
                } else if (isNumeric(orderData.getDepositId())) {
                    busId = Long.parseLong(orderData.getDepositId());
                }
                Integer servicesId = null;
                String servicesName = null;
                Long servicePrice = null;
                Integer serviceQty = null;
                if (orderData.getServices() != null && !orderData.getServices().isEmpty()) {
                    ServiceOrderReserveData reserveData = orderData.getServices().get(0);
                    servicesId = Integer.valueOf(reserveData.getServiceId());
                    servicesName = reserveData.getServiceName();
                    servicePrice = reserveData.getPrice();
                    serviceQty = reserveData.getQty();
                }

                if (exists) {
                    jdbcTemplate.update("UPDATE t_sfi_bus_collection_order_service SET bus_id = ?, order_id = ?, "
                                    + "order_no = ?, order_status = ?, pay_time = ?, pay_serial_no = ?, order_date = ?, "
                                    + "services_id = ?, services_name = ?, service_price = ?, service_qty = ?, result = ? WHERE id = ?",
                            busId, Integer.valueOf(orderData.getId()), orderData.getOrderNo(), orderData.getOrderStatus(),
                            orderData.getPayTime(), orderData.getPaySerialNo(), orderData.getDate(), servicesId,
                            servicesName, servicePrice, serviceQty, COLLECTION_SUCCESS, orderServiceId);
                } else {
                    jdbcTemplate.update("INSERT INTO t_sfi_bus_collection_order_service (id, bus_id, order_id, order_no, "
                                    + "order_status, pay_time, pay_serial_no, order_date, services_id, services_name, "
                                    + "service_price, service_qty, result) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            orderServiceId, busId, Integer.valueOf(orderData.getId()), orderData.getOrderNo(),
                            orderData.getOrderStatus(), orderData.getPayTime(), orderData.getPaySerialNo(),
                            orderData.getDate(), servicesId, servicesName, servicePrice, serviceQty, COLLECTION_SUCCESS);
                }

                updateOrders.add(new ServiceOrderUpdateData(orderData.getId(), orderData.getOrderNo(), orderData.getOrderType()));

            } catch (Exception e) {
                log.warn("[cemlink] 更新售后服务数据发生异常，orderId：{}", orderData.getId(), e);
                if (exists) {
                    jdbcTemplate.update("UPDATE t_sfi_bus_collection_order_service SET result = ? WHERE id = ?",
                            COLLECTION_ERROR, orderServiceId);
                } else {
                    jdbcTemplate.update("INSERT INTO t_sfi_bus_collection_order_service (id, order_id, order_no, result) "
                                    + "VALUES (?, ?, ?, ?)",
                            orderServiceId, Integer.valueOf(orderData.getId()), orderData.getOrderNo(), COLLECTION_ERROR);
                }
            }
        }

        if (!updateOrders.isEmpty()) {
            mallLinkoraClient.publish(CemlinkTopics.UPLINK_ORDER_UPDATE, updateOrders);
        }
        log.info("[cemlink] 售后订单处理完成，成功数：{}", updateOrders.size());
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (char c : value.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private long nextId(String table) {
        Long max = jdbcTemplate.queryForObject("SELECT IFNULL(MAX(id), 0) FROM " + table, Long.class);
        return max + 1;
    }
}
