package com.scaffold.module.system.biz.service;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.module.system.biz.entity.AccountRole;
import com.scaffold.module.system.biz.entity.SysRole;
import com.scaffold.module.system.biz.repository.AccountRoleRepository;
import com.scaffold.module.system.biz.repository.SysRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理服务：角色的创建/更新/删除与占用校验。
 *
 * <p>轻量 RBAC 的角色侧闭环——此前只有「分配角色」，角色本身靠种子数据；
 * 本服务补齐 CRUD，删除前校验无账号绑定，防止把在用角色悄悄删掉。</p>
 *
 * @author lianghang
 */
@Service
public class RoleAdminService {

    private final SysRoleRepository roleRepository;
    private final AccountRoleRepository accountRoleRepository;

    public RoleAdminService(SysRoleRepository roleRepository,
                            AccountRoleRepository accountRoleRepository) {
        this.roleRepository = roleRepository;
        this.accountRoleRepository = accountRoleRepository;
    }

    /** 创建角色：编码唯一性校验 */
    public SysRole create(String code, String name, String permissions) {
        if (roleRepository.findByCodeAndDeletedFalse(code).isPresent()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "角色编码已存在: " + code);
        }
        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(name);
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    /** 更新角色名称与权限串（编码不可改——它是权限校验的锚点） */
    public SysRole update(Long roleId, String name, String permissions) {
        SysRole role = roleRepository.findById(roleId)
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.BAD_REQUEST, "角色不存在: " + roleId));
        if (name != null && !name.isEmpty()) {
            role.setName(name);
        }
        if (permissions != null) {
            role.setPermissions(permissions);
        }
        return roleRepository.save(role);
    }

    /** 删除角色：有账号绑定时拒绝（逻辑删除，保留审计痕迹） */
    @Transactional
    public void delete(Long roleId) {
        SysRole role = roleRepository.findById(roleId)
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.BAD_REQUEST, "角色不存在: " + roleId));
        List<AccountRole> bindings = accountRoleRepository.findByRoleId(roleId);
        if (!bindings.isEmpty()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST,
                    "角色下仍有 " + bindings.size() + " 个账号绑定，请先解除分配");
        }
        role.setDeleted(true);
        roleRepository.save(role);
    }
}
