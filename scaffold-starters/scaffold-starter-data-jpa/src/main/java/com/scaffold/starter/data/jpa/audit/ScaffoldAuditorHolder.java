package com.scaffold.starter.data.jpa.audit;

/**
 * 审计人持有器，以 ThreadLocal 保存当前请求的账户标识供 JPA Auditing 读取。
 *
 * <p>由安全层（starter-security 的过滤器）在认证成功后调用 {@code set} 写入，
 * {@code AuditorAware} 读取后填充 {@code createBy/updateBy}。data-jpa 不依赖
 * security 模块，二者只约定这一个写入点，从而保持模块间解耦。</p>
 *
 * <p>请求结束时调用方必须执行 {@code clear}，防止线程复用导致审计人串号。</p>
 *
 * @author lianghang
 */
public final class ScaffoldAuditorHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

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
