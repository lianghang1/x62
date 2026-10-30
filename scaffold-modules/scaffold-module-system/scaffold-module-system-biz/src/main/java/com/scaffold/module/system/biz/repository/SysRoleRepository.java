package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 角色仓储。
 *
 * @author lianghang
 */
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {

    /** 按角色编码查询未删除角色。 */
    Optional<SysRole> findByCodeAndDeletedFalse(String code);
}
