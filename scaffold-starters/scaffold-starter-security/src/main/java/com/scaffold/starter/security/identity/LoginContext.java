package com.scaffold.starter.security.identity;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.starter.data.jpa.audit.ScaffoldAuditorHolder;

/**
 * 登录上下文——业务层获取当前身份的唯一入口。
 *
 * <p>底座使用阿里 TransmittableThreadLocal：@Async（TraceTaskDecorator）/ 自建线程池（经 core 的 ScaffoldExecutors 包装）
 * 中照样取得到「提交那一刻」的身份，且不会串号。</p>
 *
 * <p>写入与清理由三条安全链的过滤器负责（finally 强制清理，防线程复用串号）；
 * 业务代码禁止裸取 SecurityContextHolder / request attribute。</p>
 *
 * @author lianghang
 */
public final class LoginContext {

    private static final TransmittableThreadLocal<LoginUser> HOLDER = new TransmittableThreadLocal<>();

    private LoginContext() {
    }

    /** 获取当前登录用户；未登录抛 1001 未认证 */
    public static LoginUser current() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    /** 获取当前登录用户；匿名场景（/online）返回 null */
    public static LoginUser currentOrNull() {
        return HOLDER.get();
    }

    public static Long currentAccountId() {
        LoginUser user = current();
        return user.getAccountId();
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
        // 同步审计人，createBy/updateBy 自动填充
        ScaffoldAuditorHolder.set(String.valueOf(user.getAccountId()));
    }

    public static void clear() {
        HOLDER.remove();
        ScaffoldAuditorHolder.clear();
    }
}
