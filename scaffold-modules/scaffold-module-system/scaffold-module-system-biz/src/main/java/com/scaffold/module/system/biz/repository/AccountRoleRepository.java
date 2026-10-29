package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.AccountRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRoleRepository extends JpaRepository<AccountRole, Long> {

    List<AccountRole> findByAccountId(Long accountId);

    void deleteByAccountId(Long accountId);
}
