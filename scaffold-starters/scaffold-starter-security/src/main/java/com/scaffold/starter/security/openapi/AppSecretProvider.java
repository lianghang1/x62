package com.scaffold.starter.security.openapi;

/**
 * openapi 应用凭证提供者的 SPI 契约。
 *
 * <p>starter-security 只定义契约，module-system 提供基于 app_info 表的实现；
 * 未提供实现时 /openapi/** 一律拒绝（fail closed）——凭证来源是业务数据，框架不越俎代庖。</p>
 *
 * @author lianghang
 */
public interface AppSecretProvider {

    /**
     * @return appKey 对应的应用身份；不存在或已停用返回 null
     */
    AppIdentity load(String appKey);
}
