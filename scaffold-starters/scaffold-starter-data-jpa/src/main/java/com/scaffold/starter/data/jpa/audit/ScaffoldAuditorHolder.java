package com.scaffold.starter.data.jpa.audit;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 审计人持有器，以阿里 TransmittableThreadLocal 保存当前请求的账户标识供 JPA Auditing 读取。
 *
 * <p>由安全层（starter-security 的过滤器）在认证成功后调用 {@code set} 写入，
 * {@code AuditorAware} 读取后填充 {@code createBy/updateBy}。data-jpa 不依赖
 * security 模块，二者只约定这一个写入点，从而保持模块间解耦。</p>
 *
 * <p>TTL 底座：经 ScaffoldExecutors 包装的线程池 / @Async 装饰器中照样取得到
 * 「提交那一刻」的审计人，createBy/updateBy 在异步落库场景不丢失。</p>
 *
 * <p>请求结束时调用方必须执行 {@code clear}，防止线程复用导致审计人串号。</p>
 *
 * @author lianghang
 */
public final class ScaffoldAuditorHolder {

    private static final TransmittableThreadLocal<String> HOLDER = new TransmittableThreadLocal<>();

    private ScaffoldAuditorHolder() {
    }

    public static void set(String auditor) {
        HOLDER.set(auditor);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
