package com.scaffold.module.cemlink.down;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkora.client.LinkoraReceivedMessage;
import com.linkora.client.MessageListener;
import com.scaffold.module.cemlink.client.CemlinkTopics;
import com.scaffold.module.cemlink.client.MallLinkoraClient;
import com.scaffold.module.cemlink.client.model.DepositBindData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 寄存绑定下行回写：server 下发公众号绑定格位 → 写 t_sfi_ashes_bus_family_member → 回传已处理ID
 *
 * @author lianghang
 */
public class DepositBindDownlinkHandler implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(DepositBindDownlinkHandler.class);

    /** 操作人（原采集程序常量） */
    private static final int CREATE_USER_ID = 9999;
    /** 绑定备注（原采集程序常量） */
    private static final String REMARK_BIND = "（来自网络【公众号】绑定格位）";

    private final JdbcTemplate jdbcTemplate;
    private final MallLinkoraClient mallLinkoraClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DepositBindDownlinkHandler(@Qualifier("cemlinkParkJdbc") JdbcTemplate jdbcTemplate,
                                      MallLinkoraClient mallLinkoraClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.mallLinkoraClient = mallLinkoraClient;
    }

    @Override
    public void onMessage(LinkoraReceivedMessage message) {
        if (!CemlinkTopics.DOWNLINK_DEPOSIT_BIND.equals(message.getMessageType())) {
            return;
        }
        try {
            List<DepositBindData> items = objectMapper.readValue(message.getPayload(),
                    new TypeReference<List<DepositBindData>>() {
                    });
            handle(items);
        } catch (IOException e) {
            throw new IllegalStateException("寄存绑定下行数据解析失败", e);
        }
    }

    /** 原程序 updateDepositBind 逻辑平移：已存在同证件号绑定则跳过，全部回传 */
    public void handle(List<DepositBindData> bindDataList) {
        if (bindDataList == null || bindDataList.isEmpty()) {
            return;
        }
        Date operateTime = new Date();
        List<String> successIds = new ArrayList<>(bindDataList.size());
        for (DepositBindData bindData : bindDataList) {
            boolean exists = false;

            if (bindData.getUserIdNo() != null && !bindData.getUserIdNo().isEmpty()) {
                List<Long> found = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_ashes_bus_family_member WHERE business_id = ? AND certificate_no = ?",
                        Long.class, bindData.getDepositId(), bindData.getUserIdNo());
                if (!found.isEmpty()) {
                    exists = true;
                }
            }

            if (!exists) {
                long id = nextId();
                jdbcTemplate.update("INSERT INTO t_sfi_ashes_bus_family_member "
                                + "(id, business_id, name, certificate_no, mobile_no, remark, create_date, create_user, is_validate) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        id, bindData.getDepositId(),
                        bindData.getUserName() != null && !bindData.getUserName().isEmpty()
                                ? bindData.getUserName() : bindData.getIdUserName(),
                        bindData.getUserIdNo(), bindData.getIdUserPhone(), REMARK_BIND,
                        operateTime, CREATE_USER_ID, "1");
            }

            successIds.add(bindData.getId());
        }

        mallLinkoraClient.publish(CemlinkTopics.UPLINK_DEPOSIT_BIND_UPDATE, successIds);
        log.info("[cemlink] 寄存绑定处理完成，处理数：{}", successIds.size());
    }

    /** 主键取号：MAX(id) + 1（对应原程序 LongIDEntity 自增语义，客户端单实例低并发） */
    private long nextId() {
        Long max = jdbcTemplate.queryForObject("SELECT IFNULL(MAX(id), 0) FROM t_sfi_ashes_bus_family_member", Long.class);
        return max + 1;
    }
}
