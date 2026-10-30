package com.scaffold.module.system.api.api;

import com.scaffold.module.system.api.dto.AccountDTO;

/**
 * 账户查询契约。
 *
 * <p>其他业务模块依赖本接口获取账户信息，从而不感知 system-biz 的实现细节；
 * 查询不存在时返回 {@code null} 而非抛异常，由调用方自行决定兜底策略。</p>
 *
 * @author lianghang
 */
public interface AccountApi {

    /**
     * 按账户 ID 查询账户信息。
     *
     * @param accountId 账户 ID
     * @return 账户信息；不存在返回 {@code null}
     */
    AccountDTO getById(Long accountId);
}
