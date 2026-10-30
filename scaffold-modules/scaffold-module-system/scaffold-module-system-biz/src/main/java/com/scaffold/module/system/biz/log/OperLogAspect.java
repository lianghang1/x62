package com.scaffold.module.system.biz.log;

import com.scaffold.common.event.EventOps;
import com.scaffold.module.system.api.annotation.OperLog;
import com.scaffold.module.system.biz.event.OperLogEvent;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.identity.LoginUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面。
 *
 * <p>环绕拦截 {@code @OperLog} 标注的方法，执行前后落库 {@code sys_oper_log}：
 * 失败也记录（success=false + 错误摘要）后原样抛出，不影响异常处理链；
 * 日志落库自身失败仅告警——审计不能反过来拖垮主流程。</p>
 *
 * @author lianghang
 */
@Aspect
@org.springframework.stereotype.Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    private static final int PARAMS_MAX_LENGTH = 2048;
    private static final int ERROR_MAX_LENGTH = 1024;

    public OperLogAspect() {
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            save(joinPoint, operLog, System.currentTimeMillis() - start, true, null);
            return result;
        } catch (Throwable e) {
            save(joinPoint, operLog, System.currentTimeMillis() - start, false, e.getMessage());
            throw e;
        }
    }

    /** 组装事件并发布：事务提交后才发（回滚不发），AuditLogListener 异步落库。 */
    private void save(ProceedingJoinPoint joinPoint, OperLog operLog,
                      long costMillis, boolean success, String errorMessage) {
        try {
            com.scaffold.module.system.biz.entity.OperLog entry =
                    new com.scaffold.module.system.biz.entity.OperLog();
            entry.setModule(operLog.module());
            entry.setAction(operLog.action());
            entry.setCostMillis(costMillis);
            entry.setSuccess(success);
            entry.setErrorMessage(truncate(errorMessage, ERROR_MAX_LENGTH));
            LoginUser user = LoginContext.currentOrNull();
            entry.setOperator(user == null ? "anonymous" : user.getAccountNo());
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                entry.setUri(request.getRequestURI());
                entry.setIp(com.scaffold.module.system.biz.service.AuthService.clientIp(request));
            }
            entry.setParams(truncate(args(joinPoint.getArgs()), PARAMS_MAX_LENGTH));
            EventOps.publishAfterCommit(new com.scaffold.module.system.biz.event.OperLogEvent(
                    entry.getModule(), entry.getAction(), entry.getOperator(), entry.getIp(),
                    entry.getUri(), entry.getParams(), entry.getCostMillis(), entry.getSuccess(),
                    entry.getErrorMessage(), java.time.LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("[oper-log] 操作日志记录失败: {}#{}", operLog.module(), operLog.action(), e);
        }
    }

    /** 入参 JSON 化：优先 Jackson，失败退化为 toString，避免日志记录拖垮主流程。 */
    private String args(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg instanceof javax.servlet.ServletRequest
                            || arg instanceof javax.servlet.ServletResponse) {
                        return arg.getClass().getSimpleName();
                    }
                    return String.valueOf(arg);
                })
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
