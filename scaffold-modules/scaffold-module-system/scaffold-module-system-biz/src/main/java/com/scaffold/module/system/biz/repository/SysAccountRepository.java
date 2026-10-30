package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.SysAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 统一账户仓储。
 *
 * <p>查询一律带 {@code DeletedFalse} 条件——逻辑删除的账户对业务不可见，
 * 从仓储层强制过滤，避免调用方遗漏判断。</p>
 *
 * @author lianghang
 */
public interface SysAccountRepository extends JpaRepository<SysAccount, Long> {

    /** 按账户编号查询未删除账户。 */
    Optional<SysAccount> findByAccountNoAndDeletedFalse(String accountNo);

    /** 按主键查询未删除账户。 */
    Optional<SysAccount> findByIdAndDeletedFalse(Long id);
}
