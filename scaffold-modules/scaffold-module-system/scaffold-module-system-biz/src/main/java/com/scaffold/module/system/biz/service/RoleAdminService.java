package com.scaffold.module.system.biz.service;

import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.module.system.biz.entity.AccountRole;
import com.scaffold.module.system.biz.entity.SysRole;
import com.scaffold.module.system.biz.repository.AccountRoleRepository;
import com.scaffold.module.system.biz.repository.SysRoleRepository;
import com.scaffold.starter.security.token.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理服务：角色的创建/更新/删除与占用校验。
 *
 * <p>轻量 RBAC 的角色侧闭环：创建编码唯一、删除前校验无账号绑定（逻辑删除，
 * 保留审计痕迹）。内置角色 {@value #PROTECTED_ROLE_CODE} 受保护——不可删、
 * 权限串不可改，防止把超级管理员权限悄悄改掉或删掉。</p>
 *
 * <p>权限变更联动下线：会话中的权限集在登录时装入、不随角色实时刷新，
 * 因此更新角色权限串后对绑定账号执行全端下线，令新权限立即生效（重登即取新集）。</p>
 *
 * @author lianghang
 */
@Service
public class RoleAdminService {

    private static final Logger log = LoggerFactory.getLogger(RoleAdminService.class);

    /** 内置受保护角色：全量权限，不可删、权限串不可改 */
    public static final String PROTECTED_ROLE_CODE = "SUPER_ADMIN";

    private final SysRoleRepository roleRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final TokenService tokenService;

    public RoleAdminService(SysRoleRepository roleRepository,
                            AccountRoleRepository accountRoleRepository,
                            TokenService tokenService) {
        this.roleRepository = roleRepository;
        this.accountRoleRepository = accountRoleRepository;
        this.tokenService = tokenService;
    }

    /** 创建角色：编码唯一性校验 */
    public SysRole create(String code, String name, String permissions) {
        if (PROTECTED_ROLE_CODE.equals(code)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "内置角色编码不可使用: " + code);
        }
        if (roleRepository.findByCodeAndDeletedFalse(code).isPresent()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "角色编码已存在: " + code);
        }
        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(name);
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    /**
     * 更新角色名称与权限串（编码不可改——它是权限校验的锚点）。
     *
     * <p>受保护角色仅允许改名称；权限串变化后踢掉绑定账号全端下线，
     * 使新权限集在重登后生效。</p>
     */
    @Transactional
    public SysRole update(Long roleId, String name, String permissions) {
        SysRole role = roleRepository.findById(roleId)
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.BAD_REQUEST, "角色不存在: " + roleId));
        if (name != null && !name.isEmpty()) {
            role.setName(name);
        }
        boolean permissionsChanged = false;
        if (permissions != null) {
            if (PROTECTED_ROLE_CODE.equals(role.getCode()) && !permissions.equals(role.getPermissions())) {
                throw BizException.of(GlobalErrorCode.BAD_REQUEST,
                        PROTECTED_ROLE_CODE + " 权限串不可修改");
            }
            if (!permissions.equals(role.getPermissions())) {
                permissionsChanged = true;
                role.setPermissions(permissions);
            }
        }
        SysRole saved = roleRepository.save(role);
        if (permissionsChanged) {
            kickBoundAccounts(roleId);
        }
        return saved;
    }

    /** 删除角色：受保护角色与有账号绑定的角色均拒绝（逻辑删除，保留审计痕迹） */
    @Transactional
    public void delete(Long roleId) {
        SysRole role = roleRepository.findById(roleId)
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.BAD_REQUEST, "角色不存在: " + roleId));
        if (PROTECTED_ROLE_CODE.equals(role.getCode())) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, PROTECTED_ROLE_CODE + " 为内置角色，不可删除");
        }
        List<AccountRole> bindings = accountRoleRepository.findByRoleId(roleId);
        if (!bindings.isEmpty()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST,
                    "角色下仍有 " + bindings.size() + " 个账号绑定，请先解除分配");
        }
        role.setDeleted(true);
        roleRepository.save(role);
    }

    /** 权限变更后踢掉绑定账号：会话权限集在登录时装入，重登才取新集 */
    private void kickBoundAccounts(Long roleId) {
        for (AccountRole binding : accountRoleRepository.findByRoleId(roleId)) {
            tokenService.revokeAll(binding.getAccountId());
            log.info("[role-admin] 角色权限变更，已下线绑定账号: roleId={}, accountId={}",
                    roleId, binding.getAccountId());
        }
    }
}
