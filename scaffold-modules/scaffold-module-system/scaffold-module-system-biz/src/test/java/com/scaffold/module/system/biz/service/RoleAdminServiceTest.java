package com.scaffold.module.system.biz.service;

import com.scaffold.common.exception.BizException;
import com.scaffold.module.system.biz.entity.AccountRole;
import com.scaffold.module.system.biz.entity.SysRole;
import com.scaffold.module.system.biz.repository.AccountRoleRepository;
import com.scaffold.module.system.biz.repository.SysRoleRepository;
import com.scaffold.starter.security.token.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色管理服务测试：编码唯一、内置角色保护（不可删/权限串不可改）、
 * 权限变更联动踢人、删除占用校验。
 *
 * @author lianghang
 */
class RoleAdminServiceTest {

    private final SysRoleRepository roleRepository = mock(SysRoleRepository.class);
    private final AccountRoleRepository accountRoleRepository = mock(AccountRoleRepository.class);
    private final TokenService tokenService = mock(TokenService.class);

    private RoleAdminService service;

    @BeforeEach
    void setUp() {
        service = new RoleAdminService(roleRepository, accountRoleRepository, tokenService);
    }

    private SysRole role(String code, String permissions) {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setCode(code);
        role.setName("演示角色");
        role.setPermissions(permissions);
        return role;
    }

    @Test
    void createShouldRejectProtectedCode() {
        assertThrows(BizException.class, () -> service.create("SUPER_ADMIN", "超管", "*"));
    }

    @Test
    void createShouldRejectDuplicateCode() {
        when(roleRepository.findByCodeAndDeletedFalse("ops")).thenReturn(Optional.of(role("ops", "")));
        assertThrows(BizException.class, () -> service.create("ops", "运营", ""));
    }

    @Test
    void updateShouldRejectPermissionChangeOnProtectedRole() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role("SUPER_ADMIN", "*")));
        assertThrows(BizException.class, () -> service.update(1L, null, "system:account:manage"));
    }

    @Test
    void deleteShouldRejectProtectedRole() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role("SUPER_ADMIN", "*")));
        assertThrows(BizException.class, () -> service.delete(1L));
    }

    @Test
    void deleteShouldRejectWhenAccountsBound() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("ops", "")));
        when(accountRoleRepository.findByRoleId(2L))
                .thenReturn(Collections.singletonList(new AccountRole()));
        assertThrows(BizException.class, () -> service.delete(2L));
    }

    @Test
    void updatePermissionsShouldKickBoundAccounts() {
        SysRole existing = role("ops", "system:dict:manage");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(SysRole.class))).thenAnswer(inv -> inv.getArgument(0));
        AccountRole binding = new AccountRole();
        binding.setAccountId(9L);
        when(accountRoleRepository.findByRoleId(2L)).thenReturn(Collections.singletonList(binding));

        service.update(2L, null, "system:dict:manage,system:audit:query");

        verify(tokenService, times(1)).revokeAll(9L);
        assertEquals("system:dict:manage,system:audit:query", existing.getPermissions());
    }

    @Test
    void updateWithSamePermissionsShouldNotKick() {
        SysRole existing = role("ops", "system:dict:manage");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(SysRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRoleRepository.findByRoleId(anyLong())).thenReturn(Collections.emptyList());

        service.update(2L, "新名称", "system:dict:manage");

        verify(tokenService, never()).revokeAll(anyLong());
    }
}
