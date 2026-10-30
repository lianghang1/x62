package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.AppInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 三方应用扩展信息仓储。
 *
 * <p>appKey 是三方调用的身份凭证，按其定位应用是签名校验的热路径；
 * 其余查询排除逻辑删除记录。</p>
 *
 * @author lianghang
 */
public interface AppInfoRepository extends JpaRepository<AppInfo, Long> {

    /** 按 appKey 查询未删除的应用。 */
    Optional<AppInfo> findByAppKeyAndDeletedFalse(String appKey);

    /** 按账户 ID 查询未删除的应用。 */
    Optional<AppInfo> findByAccountIdAndDeletedFalse(Long accountId);

    /** 按主键查询未删除的应用。 */
    Optional<AppInfo> findByIdAndDeletedFalse(Long id);
}
