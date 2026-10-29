package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysRoleRepository extends JpaRepository<SysRole, Long> {

    Optional<SysRole> findByCodeAndDeletedFalse(String code);
}
