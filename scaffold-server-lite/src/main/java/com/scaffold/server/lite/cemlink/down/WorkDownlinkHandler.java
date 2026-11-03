package com.scaffold.server.lite.cemlink.down;

import com.fasterxml.jackson.core.type.TypeReference;
import com.linkora.client.LinkoraReceivedMessage;
import com.linkora.client.MessageListener;
import com.scaffold.common.util.JsonUtils;
import com.scaffold.server.lite.cemlink.client.CemlinkTopics;
import com.scaffold.server.lite.cemlink.client.MallLinkoraClient;
import com.scaffold.server.lite.cemlink.client.model.WorkCheckData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import com.scaffold.starter.data.jpa.datasource.TargetDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 工单验收下行回写：server 下发待验收工单 → 写 t_sfi_work_pass + 置客户验收状态 → 回传已处理ID
 *
 * @author lianghang
 */
@TargetDataSource("park")
public class WorkDownlinkHandler implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(WorkDownlinkHandler.class);

    /** 验收操作人（原采集程序常量） */
    private static final int CREATE_USER_ID = 9999;
    private static final String CREATE_USER = "系统";
    /** 工单状态：客户验收（原采集程序 WorkStatus 平移） */
    private static final String WORK_STATUS_CHECKED = "4";

    private final JdbcTemplate jdbcTemplate;
    private final MallLinkoraClient mallLinkoraClient;

    public WorkDownlinkHandler(@Qualifier("cemlinkParkJdbc") JdbcTemplate jdbcTemplate,
                               MallLinkoraClient mallLinkoraClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.mallLinkoraClient = mallLinkoraClient;
    }

    @Override
    public void onMessage(LinkoraReceivedMessage message) {
        if (!CemlinkTopics.DOWNLINK_WORK_CHECK.equals(message.getMessageType())) {
            return;
        }
        try {
            List<WorkCheckData> items = JsonUtils.fromJson(new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8), new TypeReference<List<WorkCheckData>>() {
            });
            handle(items);
        } catch (Exception e) {
            throw new IllegalStateException("工单验收下行数据解析失败", e);
        }
    }

    /** 原程序 saveWorkInfo 逻辑平移：单条失败告警跳过，成功列表回传 */
    public void handle(List<WorkCheckData> workDataList) {
        if (workDataList == null || workDataList.isEmpty()) {
            return;
        }
        Date operateTime = new Date();
        List<String> updateWorks = new ArrayList<>(workDataList.size());
        for (WorkCheckData workData : workDataList) {
            try {
                if (!workData.getWorkId().chars().allMatch(Character::isDigit) || workData.getWorkId().isEmpty()) {
                    continue;
                }
                long workId = Long.parseLong(workData.getWorkId());
                List<Long> exists = jdbcTemplate.queryForList(
                        "SELECT id FROM t_sfi_work WHERE id = ?", Long.class, workId);
                if (exists.isEmpty()) {
                    continue;
                }

                long workPassId = nextId("t_sfi_work_pass");
                jdbcTemplate.update("INSERT INTO t_sfi_work_pass "
                                + "(id, work_id, operate_date, operate_user_id, operate_user, operate_version, "
                                + "check_status, create_date, create_user, is_validate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        workPassId, workId, operateTime, CREATE_USER_ID, CREATE_USER, 1,
                        "1", operateTime, CREATE_USER_ID, "1");

                jdbcTemplate.update("UPDATE t_sfi_work SET work_status = ?, modify_date = ?, modify_user = ? WHERE id = ?",
                        WORK_STATUS_CHECKED, operateTime, CREATE_USER_ID, workId);

                updateWorks.add(workData.getId());
            } catch (Exception e) {
                log.warn("[cemlink] 更新售后服务数据发生异常，workId：{}", workData.getWorkId(), e);
            }
        }

        if (!updateWorks.isEmpty()) {
            mallLinkoraClient.publish(CemlinkTopics.UPLINK_WORK_UPDATE, updateWorks);
        }
        log.info("[cemlink] 工单验收处理完成，成功数：{}", updateWorks.size());
    }

    /** 主键取号：MAX(id) + 1（对应原程序 LongIDEntity 自增语义，客户端单实例低并发） */
    private long nextId(String table) {
        Long max = jdbcTemplate.queryForObject("SELECT IFNULL(MAX(id), 0) FROM " + table, Long.class);
        return max + 1;
    }
}
