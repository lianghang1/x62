package com.scaffold.module.system.biz;

import com.scaffold.common.exception.BizException;
import com.scaffold.module.system.biz.entity.SysAccount;
import com.scaffold.module.system.biz.repository.SysAccountRepository;
import com.scaffold.module.system.biz.service.AccountAdminService;
import com.scaffold.starter.security.identity.AccountType;
import com.scaffold.starter.security.policy.PasswordPolicyValidator;
import com.scaffold.starter.security.token.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 账号管理服务测试：创建校验、启停用联动下线、重置密码一次性语义。
 *
 * @author lianghang
 */
class AccountAdminServiceTest {

    private final SysAccountRepository repository = mock(SysAccountRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final PasswordPolicyValidator policyValidator = mock(PasswordPolicyValidator.class);
    private final TokenService tokenService = mock(TokenService.class);

    private AccountAdminService service;

    @BeforeEach
    void setUp() {
        service = new AccountAdminService(repository, passwordEncoder, policyValidator, tokenService);
    }

    private SysAccount operator() {
        SysAccount account = new SysAccount();
        account.setId(1L);
        account.setAccountNo("op01");
        account.setType(AccountType.OPERATOR);
        account.setStatus("ENABLE");
        return account;
    }

    @Test
    void createOperatorRejectsDuplicate() {
        when(repository.findByAccountNoAndDeletedFalse("op01")).thenReturn(Optional.of(operator()));

        assertThrows(BizException.class, () -> service.createOperator("op01", "abc12345"));
        verify(repository, never()).save(any(SysAccount.class));
    }

    @Test
    void createOperatorValidatesPasswordPolicyAndEncodes() {
        when(repository.findByAccountNoAndDeletedFalse("op02")).thenReturn(Optional.empty());
        when(repository.save(any(SysAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("abc12345")).thenReturn("$2a$encoded");

        service.createOperator("op02", "abc12345");

        verify(policyValidator).validate("abc12345");
        ArgumentCaptor<SysAccount> captor = ArgumentCaptor.forClass(SysAccount.class);
        verify(repository).save(captor.capture());
        assertEquals(AccountType.OPERATOR, captor.getValue().getType());
        assertEquals("$2a$encoded", captor.getValue().getPassword());
    }

    @Test
    void disableAccountKicksAllSessions() {
        when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(operator()));
        when(repository.save(any(SysAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, false);
        // 停用踢全端
        verify(tokenService, Mockito.times(1)).revokeAll(1L);

        // 启用不再追加踢人（仍只有停用那一次）
        service.changeStatus(1L, true);
        verify(tokenService, Mockito.times(1)).revokeAll(1L);
    }

    @Test
    void resetPasswordReturnsPlaintextOnceAndKicks() {
        SysAccount account = operator();
        account.setPassword("$2a$old");
        when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(account));
        when(repository.save(any(SysAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("$2a$new");

        String plaintext = service.resetPassword(1L);

        assertNotNull(plaintext);
        assertEquals(12, plaintext.length());
        assertNotEquals("$2a$old", plaintext);
        verify(tokenService).revokeAll(1L);
        ArgumentCaptor<SysAccount> captor = ArgumentCaptor.forClass(SysAccount.class);
        verify(repository).save(captor.capture());
        assertEquals("$2a$new", captor.getValue().getPassword());
    }

    @Test
    void pageRejectsMissingAccountWith404() {
        when(repository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> service.changeStatus(404L, true));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageForcesDeletedFalseCondition() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        service.page(null, null, null, 1, 10);

        ArgumentCaptor<Specification<SysAccount>> captor =
                ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository).findAll(captor.capture(), any(Pageable.class));
        assertNotNull(captor.getValue());
    }
}
