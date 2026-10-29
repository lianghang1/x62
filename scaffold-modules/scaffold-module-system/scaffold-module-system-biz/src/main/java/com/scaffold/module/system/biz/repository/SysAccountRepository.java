package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.SysAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysAccountRepository extends JpaRepository<SysAccount, Long> {

    Optional<SysAccount> findByAccountNoAndDeletedFalse(String accountNo);

    Optional<SysAccount> findByIdAndDeletedFalse(Long id);
}
