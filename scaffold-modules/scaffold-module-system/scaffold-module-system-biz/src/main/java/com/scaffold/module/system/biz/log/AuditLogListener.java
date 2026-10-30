package com.scaffold.module.system.biz.log;

import com.scaffold.module.system.biz.entity.LoginLog;
import com.scaffold.module.system.biz.entity.OperLog;
import com.scaffold.module.system.biz.event.LoginLogEvent;
import com.scaffold.module.system.biz.event.OperLogEvent;
import com.scaffold.module.system.biz.repository.LoginLogRepository;
import com.scaffold.module.system.biz.repository.OperLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计日志异步监听器：登录日志与操作日志的统一落库出口。
 *
 * <p>生产方（AuthService/OperLogAspect）只负责在请求线程取齐上下文并发布事件；
 * 本类 {@code @Async} 消费，写库失败仅告警——审计尽力而为，不拖垮主流程。
 * 执行器走 starter-web 的 {@code applicationTaskExecutor}（含 traceId/TTL 装饰器）。</p>
 *
 * @author lianghang
 */
@Component
public class AuditLogListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogListener.class);

    private final LoginLogRepository loginLogRepository;
    private final OperLogRepository operLogRepository;

    public AuditLogListener(LoginLogRepository loginLogRepository, OperLogRepository operLogRepository) {
        this.loginLogRepository = loginLogRepository;
        this.operLogRepository = operLogRepository;
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
}
