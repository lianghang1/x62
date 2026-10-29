package com.scaffold.module.system.api.api;

import com.scaffold.module.system.api.dto.AccountDTO;

/**
 * 账户查询契约：其他业务模块依赖本接口，不感知 system-biz 实现细节。
 */
public interface AccountApi {

    /**
     * @return 账户信息；不存在返回 null
     */
    AccountDTO getById(Long accountId);
}
