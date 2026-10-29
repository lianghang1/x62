package com.scaffold.starter.data.jpa.audit;

/**
 * 审计人持有器：由安全层（starter-security 的过滤器）在认证成功后写入当前账户标识，
 * JPA Auditing 通过 AuditorAware 读取并填充 createBy/updateBy。
 * 与 security 模块解耦——data-jpa 不依赖 security，只约定这个写入点。
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
