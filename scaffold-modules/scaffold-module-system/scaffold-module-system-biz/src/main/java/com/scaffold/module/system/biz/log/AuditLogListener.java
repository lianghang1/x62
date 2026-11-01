package com.scaffold.module.system.biz.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.module.system.biz.entity.ChangeLog;
import com.scaffold.module.system.biz.entity.LoginLog;
import com.scaffold.module.system.biz.entity.OperLog;
import com.scaffold.module.system.biz.event.LoginLogEvent;
import com.scaffold.module.system.biz.event.OperLogEvent;
import com.scaffold.module.system.biz.repository.ChangeLogRepository;
import com.scaffold.module.system.biz.repository.LoginLogRepository;
import com.scaffold.module.system.biz.repository.OperLogRepository;
import com.scaffold.starter.data.jpa.datachange.DataChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计日志异步监听器：登录日志、操作日志与数据变更留痕的统一落库出口。
 *
 * <p>生产方（AuthService/OperLogAspect/DataChangeEventListener）只负责在请求
 * 线程取齐上下文并发布事件；本类 {@code @Async} 消费，写库失败仅告警——审计
 * 尽力而为，不拖垮主流程。执行器走 starter-web 的 {@code applicationTaskExecutor}
 * （含 traceId/TTL 装饰器）。</p>
 *
 * @author lianghang
 */
@Component
public class AuditLogListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogListener.class);

    /** 变更明细 JSON 上限（单行 50 字段 × 300 字符 + 结构开销，4 万保险） */
    private static final int CHANGES_MAX_LENGTH = 40000;

    private final LoginLogRepository loginLogRepository;
    private final OperLogRepository operLogRepository;
    private final ChangeLogRepository changeLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogListener(LoginLogRepository loginLogRepository,
                            OperLogRepository operLogRepository,
                            ChangeLogRepository changeLogRepository,
                            ObjectMapper objectMapper) {
        this.loginLogRepository = loginLogRepository;
        this.operLogRepository = operLogRepository;
        this.changeLogRepository = changeLogRepository;
        this.objectMapper = objectMapper;
    }

    /** 登录日志异步落库。 */
    @Async
    @EventListener
    public void onLoginLog(LoginLogEvent event) {
        try {
            LoginLog entry = new LoginLog();
            entry.setAccountNo(event.getAccountNo());
            entry.setLoginType(event.getLoginType());
            entry.setSuccess(event.isSuccess());
            entry.setFailReason(event.getFailReason());
            entry.setIp(event.getIp());
            entry.setUserAgent(event.getUserAgent());
            entry.setLoginTime(event.getLoginTime());
            loginLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("[audit] 登录日志落库失败: account={}", event.getAccountNo(), e);
        }
    }

    /** 操作日志异步落库。 */
    @Async
    @EventListener
    public void onOperLog(OperLogEvent event) {
        try {
            OperLog entry = new OperLog();
            entry.setModule(event.getModule());
            entry.setAction(event.getAction());
            entry.setOperator(event.getOperator());
            entry.setIp(event.getIp());
            entry.setUri(event.getUri());
            entry.setParams(event.getParams());
            entry.setCostMillis(event.getCostMillis());
            entry.setSuccess(event.isSuccess());
            entry.setErrorMessage(event.getErrorMessage());
            operLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("[audit] 操作日志落库失败: {}#{}",
                    event.getModule(), event.getAction(), e);
        }
    }

    /** 数据变更留痕异步落库：明细 JSON 化，失败仅告警。 */
    @Async
    @EventListener
    public void onDataChange(DataChangedEvent event) {
        try {
            ChangeLog entry = new ChangeLog();
            entry.setBusinessName(event.getBusinessName());
            entry.setTableName(event.getTableName());
            entry.setRecordId(event.getRecordId());
            entry.setOperateType(event.getOperateType().name());
            entry.setChanges(truncate(objectMapper.writeValueAsString(event.getChanges())));
            entry.setOperator(event.getOperator());
            entry.setTraceId(event.getTraceId());
            entry.setOccurredAt(event.getOccurredAt());
            changeLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("[audit] 数据变更留痕落库失败: {}#{}",
                    event.getTableName(), event.getRecordId(), e);
        }
    }

    private String truncate(String value) {
        return value == null || value.length() <= CHANGES_MAX_LENGTH
                ? value : value.substring(0, CHANGES_MAX_LENGTH) + "...";
    }
}
