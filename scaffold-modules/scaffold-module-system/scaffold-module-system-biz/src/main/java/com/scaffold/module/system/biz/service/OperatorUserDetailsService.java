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
 * 操作员账户细节装载器。
 *
 * <p>为运营者登录提供密码校验与角色 / 权限装载——RBAC 轻量实现：
 * 账户到角色再到权限串；已逻辑删除的角色在装载时被过滤，
 * 权限随角色实时生效，无需刷新登录态。</p>
 *
 * @author lianghang
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

    /** 校验明文密码与 BCrypt 摘要是否匹配；未设置密码一律不匹配。 */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encodedPassword != null && passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /** 明文密码加密为 BCrypt 摘要。 */
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /** 装载账户的角色集合，前缀 {@code ROLE_} 供 Spring Security 使用。 */
    public Set<String> loadRoles(Long accountId) {
        Set<String> roles = new HashSet<>();
        for (AccountRole ar : accountRoleRepository.findByAccountId(accountId)) {
            roleRepository.findById(ar.getRoleId())
                    .filter(r -> r.getDeleted() == null || !r.getDeleted())
                    .ifPresent(r -> roles.add("ROLE_" + r.getCode()));
        }
        return roles;
    }

    /** 装载账户的权限串集合（来自其全部有效角色）。 */
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

    /** 给操作员分配角色，全量覆盖式替换旧绑定。 */
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
