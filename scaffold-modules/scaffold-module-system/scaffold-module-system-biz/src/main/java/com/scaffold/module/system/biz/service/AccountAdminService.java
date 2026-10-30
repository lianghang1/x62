package com.scaffold.module.system.biz.service;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.core.result.PageResult;
import com.scaffold.module.system.biz.entity.SysAccount;
import com.scaffold.module.system.biz.repository.SysAccountRepository;
import com.scaffold.starter.security.identity.AccountType;
import com.scaffold.starter.security.policy.PasswordPolicyValidator;
import com.scaffold.starter.security.token.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 账号管理服务（管理端）：统一账户的 CRUD 与凭证治理。
 *
 * <p>补齐「能登录、能踢人」之外的管理闭环：分页查询、创建操作员
 * （密码强度经 {@link PasswordPolicyValidator} 把关 + BCrypt 落库）、
 * 启停用（停用即全端下线，与 {@code AccountServiceImpl} 的凭证复核呼应）、
 * 重置密码（随机密码明文仅此一次返回）。MEMBER/APP 账号不在此管理——
 * 会员由三方登录自动注册，应用走三方应用建档。</p>
 *
 * @author lianghang
 */
@Service
public class AccountAdminService {

    private static final Logger log = LoggerFactory.getLogger(AccountAdminService.class);

    /** 重置密码的随机段长度（字母 + 数字，满足默认强度策略） */
    private static final String RANDOM_ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /** 随机源（重置密码为低频操作，静态复用即可） */
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private final SysAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final TokenService tokenService;

    public AccountAdminService(SysAccountRepository accountRepository,
                               PasswordEncoder passwordEncoder,
                               PasswordPolicyValidator passwordPolicyValidator,
                               TokenService tokenService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.tokenService = tokenService;
    }

    /**
     * 分页查询账户（账号模糊 + 类型 + 状态组合过滤）。
     *
     * <p>不返回密码摘要；分页在仓储层完成，杜绝内存分页。</p>
     */
    public PageResult<Map<String, Object>> page(String accountNo, AccountType type, String status,
                                                int pageNum, int pageSize) {
        Specification<SysAccount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (StringUtils.hasText(accountNo)) {
                predicates.add(cb.like(root.get("accountNo"), "%" + accountNo + "%"));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SysAccount> page = accountRepository.findAll(spec,
                PageRequest.of(Math.max(pageNum - 1, 0), Math.min(Math.max(pageSize, 1), 200),
                        Sort.by(Sort.Direction.DESC, "id")));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysAccount account : page.getContent()) {
            list.add(toView(account));
        }
        return PageResult.of(list, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 创建操作员账号（OPERATOR）。
     *
     * @param accountNo   登录账号（唯一）
     * @param rawPassword 明文密码（强度经策略校验）
     * @return 账号视图（不含密码）
     */
    @Transactional
    public Map<String, Object> createOperator(String accountNo, String rawPassword) {
        if (!StringUtils.hasText(accountNo)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "账号不能为空");
        }
        if (accountRepository.findByAccountNoAndDeletedFalse(accountNo).isPresent()) {
            throw BizException.of(GlobalErrorCode.CONFLICT, "账号已存在: " + accountNo);
        }
        passwordPolicyValidator.validate(rawPassword);
        SysAccount account = new SysAccount();
        account.setAccountNo(accountNo);
        account.setType(AccountType.OPERATOR);
        account.setStatus("ENABLE");
        account.setPassword(passwordEncoder.encode(rawPassword));
        account = accountRepository.save(account);
        log.info("[account-admin] 创建操作员: id={}, accountNo={}", account.getId(), accountNo);
        return toView(account);
    }

    /**
     * 启用/停用账号；停用即全端下线（会话立即失效）。
     *
     * @param accountId 账号 ID
     * @param enable    true=启用 false=停用
     */
    @Transactional
    public void changeStatus(Long accountId, boolean enable) {
        SysAccount account = loadAccount(accountId);
        account.setStatus(enable ? "ENABLE" : "DISABLE");
        accountRepository.save(account);
        if (!enable) {
            // 停用立即下线：与 AppSecretProvider 的启用状态复核双层兜底
            tokenService.revokeAll(accountId);
        }
        log.info("[account-admin] 变更账号状态: id={}, accountNo={}, status={}",
                accountId, account.getAccountNo(), account.getStatus());
    }

    /**
     * 管理员重置密码：生成随机密码，明文仅本次响应返回（BCrypt 落库），并全端下线强制重登。
     *
     * @return 一次性明文密码
     */
    @Transactional
    public String resetPassword(Long accountId) {
        SysAccount account = loadAccount(accountId);
        String rawPassword = randomPassword();
        account.setPassword(passwordEncoder.encode(rawPassword));
        accountRepository.save(account);
        tokenService.revokeAll(accountId);
        log.info("[account-admin] 重置密码: id={}, accountNo={}", accountId, account.getAccountNo());
        return rawPassword;
    }

    /** 查询未删除账号，不存在抛 404。 */
    private SysAccount loadAccount(Long accountId) {
        return accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "账号不存在: " + accountId));
    }

    /** 生成满足默认强度策略的随机密码（长度 12，字母 + 数字混合，SecureRandom 防预测） */
    private String randomPassword() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(RANDOM_ALPHABET.charAt(SECURE_RANDOM.nextInt(RANDOM_ALPHABET.length())));
        }
        return builder.toString();
    }

    /** 账号视图（绝不携带密码摘要字段） */
    private Map<String, Object> toView(SysAccount account) {
        java.util.LinkedHashMap<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("id", account.getId());
        view.put("accountNo", account.getAccountNo());
        view.put("type", account.getType());
        view.put("status", account.getStatus());
        view.put("createTime", account.getCreateTime());
        return view;
    }
}
