package com.scaffold.server.lite.cemlink.down;

import com.fasterxml.jackson.core.type.TypeReference;
import com.linkora.client.LinkoraReceivedMessage;
import com.linkora.client.MessageListener;
import com.scaffold.common.util.JsonUtils;
import com.scaffold.server.lite.cemlink.client.CemlinkTopics;
import com.scaffold.server.lite.cemlink.client.MallLinkoraClient;
import com.scaffold.server.lite.cemlink.client.model.ServiceItemData;
import com.scaffold.server.lite.cemlink.client.model.TombServiceItemData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import com.scaffold.starter.data.jpa.datasource.TargetDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 微商服务项目下行回写：server 下发无编码项目 → 写入 t_sfi_code_service → 回传编码
 *
 * @author lianghang
 */
@TargetDataSource("park")
public class ServiceItemDownlinkHandler implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(ServiceItemDownlinkHandler.class);

    /** 服务类型名称（原采集程序常量） */
    private static final String SERVICE_TYPE_NAME = "O2O";

    private final JdbcTemplate jdbcTemplate;
    private final MallLinkoraClient mallLinkoraClient;

    public ServiceItemDownlinkHandler(@Qualifier("cemlinkParkJdbc") JdbcTemplate jdbcTemplate,
                                      MallLinkoraClient mallLinkoraClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.mallLinkoraClient = mallLinkoraClient;
    }

    @Override
    public void onMessage(LinkoraReceivedMessage message) {
        if (!CemlinkTopics.DOWNLINK_SERVICE_ITEM.equals(message.getMessageType())) {
            return;
        }
        try {
            List<ServiceItemData> items = JsonUtils.fromJson(new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8), new TypeReference<List<ServiceItemData>>() {
            });
            handle(items);
        } catch (Exception e) {
            throw new IllegalStateException("服务项目下行数据解析失败", e);
        }
    }

    /** 原程序 saveServiceItem 逻辑平移：写入本地服务项目并回传编码 */
    public void handle(List<ServiceItemData> serviceItemDataList) {
        if (serviceItemDataList == null || serviceItemDataList.isEmpty()) {
            return;
        }
        Date operateTime = new Date();
        Long serviceTypeId = loadOrCreateServiceType(operateTime);

        List<TombServiceItemData> updates = new ArrayList<>(serviceItemDataList.size());
        for (ServiceItemData serviceItemData : serviceItemDataList) {
            BigDecimal price = new BigDecimal(serviceItemData.getPrice()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            long serviceId = nextId("t_sfi_code_service");
            jdbcTemplate.update("INSERT INTO t_sfi_code_service "
                            + "(id, service_type_id, name, price, create_date, is_validate) VALUES (?, ?, ?, ?, ?, ?)",
                    serviceId, serviceTypeId.intValue(), serviceItemData.getName(), price.toString(),
                    operateTime, serviceItemData.getStatus() != null ? serviceItemData.getStatus() : "1");

            TombServiceItemData update = new TombServiceItemData();
            update.setId(serviceItemData.getId());
            update.setName(serviceItemData.getName());
            update.setPrice(serviceItemData.getPrice());
            update.setStatus(serviceItemData.getStatus());
            update.setCode(String.valueOf(serviceId));
            updates.add(update);
        }

        mallLinkoraClient.publish(CemlinkTopics.UPLINK_SERVICE_ITEM_UPDATE, updates);
        log.info("[cemlink] 微商服务项目编码回传成功，更新数：{}", updates.size());
    }

    /** 查找 O2O 服务类型，无则创建（原程序 codeServiceTypeDao.findAllByName 语义） */
    private Long loadOrCreateServiceType(Date operateTime) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM t_sfi_code_service_type WHERE name = ?", Long.class, SERVICE_TYPE_NAME);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        long id = nextId("t_sfi_code_service_type");
        jdbcTemplate.update("INSERT INTO t_sfi_code_service_type (id, name, create_date, is_validate) VALUES (?, ?, ?, '1')",
                id, SERVICE_TYPE_NAME, operateTime);
        return id;
    }

    /** 主键取号：MAX(id) + 1（对应原程序 LongIDEntity 自增语义，客户端单实例低并发） */
    private long nextId(String table) {
        Long max = jdbcTemplate.queryForObject("SELECT IFNULL(MAX(id), 0) FROM " + table, Long.class);
        return max + 1;
    }
}
