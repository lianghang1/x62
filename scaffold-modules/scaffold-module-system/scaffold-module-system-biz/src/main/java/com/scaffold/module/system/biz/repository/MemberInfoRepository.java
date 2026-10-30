package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.MemberInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 会员扩展信息仓储。
 *
 * <p>微信侧登录与资料查询的入口：按 openId 定位会员、按 accountId
 * 反查扩展信息，同样排除逻辑删除记录。</p>
 *
 * @author lianghang
 */
public interface MemberInfoRepository extends JpaRepository<MemberInfo, Long> {

    /** 按 openId 查询未删除的会员扩展信息。 */
    Optional<MemberInfo> findByOpenIdAndDeletedFalse(String openId);

    /** 按账户 ID 查询未删除的会员扩展信息。 */
    Optional<MemberInfo> findByAccountIdAndDeletedFalse(Long accountId);
}
