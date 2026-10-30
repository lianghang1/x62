package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.SysAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 统一账户仓储。
 *
 * <p>查询一律带 {@code DeletedFalse} 条件——逻辑删除的账户对业务不可见，
 * 从仓储层强制过滤，避免调用方遗漏判断；管理端组合条件分页走
 * {@link JpaSpecificationExecutor}（Specification 内同样强制 deleted=false）。</p>
 *
 * @author lianghang
 */
public interface SysAccountRepository extends JpaRepository<SysAccount, Long>,
        JpaSpecificationExecutor<SysAccount> {

    /** 按账户编号查询未删除账户。 */
    Optional<SysAccount> findByAccountNoAndDeletedFalse(String accountNo);

    /** 按主键查询未删除账户。 */
    Optional<SysAccount> findByIdAndDeletedFalse(Long id);
}
