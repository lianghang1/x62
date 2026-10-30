package com.scaffold.core.crypto;

/**
 * {@code DataCipher} 的静态持有器。
 *
 * <p>starter-crypto 自动装配成功后调用 {@code install}，供无法走依赖注入的场景读取——
 * 如 JPA AttributeConverter 由 Hibernate 反射实例化、并非 Spring Bean；
 * 未引入 starter-crypto 时 {@code get()} 会给出明确报错，而不是晦涩难查的 NPE。</p>
 *
 * @author lianghang
 */
public final class DataCipherHolder {

    private static volatile DataCipher cipher;

    private DataCipherHolder() {
    }

    /** 安装加密器（由 starter-crypto 自动装配时调用，仅一次）。 */
    public static void install(DataCipher dataCipher) {
        cipher = dataCipher;
    }

    /** 获取已安装的加密器；未安装时抛出带修复指引的异常。 */
    public static DataCipher get() {
        DataCipher c = cipher;
        if (c == null) {
            throw new IllegalStateException(
                    "DataCipher 未安装：请引入 scaffold-starter-crypto 并配置 scaffold.crypto.key（密码学密钥走环境变量，勿硬编码）");
        }
        return c;
    }
}
