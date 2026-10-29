package com.scaffold.module.system.biz.service;

import com.scaffold.module.system.biz.entity.AccountRole;
import com.scaffold.module.system.biz.repository.AccountRoleRepository;
import com.scaffold.module.system.biz.repository.SysRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 操作员账户细节：密码校验 + 角色/权限装载（RBAC 轻量实现：角色 -> 权限串）。
 */
@Service
public class OperatorUserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final SysRoleRepository roleRepository;
    private final AccountRoleRepository accountRoleRepository;

    public OperatorUserDetailsService(PasswordEncoder passwordEncoder,
                                      SysRoleRepository roleRepository,
                                      AccountRoleRepository accountRoleRepository) {
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.accountRoleRepository = accountRoleRepository;
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return encodedPassword != null && passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public Set<String> loadRoles(Long accountId) {
        Set<String> roles = new HashSet<>();
        for (AccountRole ar : accountRoleRepository.findByAccountId(accountId)) {
            roleRepository.findById(ar.getRoleId())
                    .filter(r -> r.getDeleted() == null || !r.getDeleted())
                    .ifPresent(r -> roles.add("ROLE_" + r.getCode()));
        }
        return roles;
    }

    public Set<String> loadPermissions(Long accountId) {
        Set<String> permissions = new HashSet<>();
        for (AccountRole ar : accountRoleRepository.findByAccountId(accountId)) {
            roleRepository.findById(ar.getRoleId())
                    .filter(r -> r.getDeleted() == null || !r.getDeleted())
                    .ifPresent(r -> {
                        if (r.getPermissions() != null && !r.getPermissions().isEmpty()) {
                            Collections.addAll(permissions, r.getPermissions().split(","));
                        }
                    });
        }
        return permissions;
    }

    /** 给操作员分配角色（全量覆盖） */
    @org.springframework.transaction.annotation.Transactional
    public void assignRoles(Long accountId, List<Long> roleIds) {
        accountRoleRepository.deleteByAccountId(accountId);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                AccountRole ar = new AccountRole();
                ar.setAccountId(accountId);
                ar.setRoleId(roleId);
                accountRoleRepository.save(ar);
            }
        }
    }
}
