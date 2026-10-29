package com.scaffold.module.system.biz.service;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.module.system.api.api.AccountApi;
import com.scaffold.module.system.api.dto.AccountDTO;
import com.scaffold.module.system.biz.convert.AccountConverter;
import com.scaffold.module.system.biz.entity.AppInfo;
import com.scaffold.module.system.biz.entity.MemberInfo;
import com.scaffold.module.system.biz.entity.SysAccount;
import com.scaffold.module.system.biz.repository.AppInfoRepository;
import com.scaffold.module.system.biz.repository.MemberInfoRepository;
import com.scaffold.module.system.biz.repository.SysAccountRepository;
import com.scaffold.starter.security.identity.AccountType;
import com.scaffold.starter.security.openapi.AppIdentity;
import com.scaffold.starter.security.openapi.AppSecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 账户服务 + openapi 凭证提供者实现。
 */
@Service
public class AccountServiceImpl implements AccountApi, AppSecretProvider {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final SysAccountRepository accountRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final AppInfoRepository appInfoRepository;

    public AccountServiceImpl(SysAccountRepository accountRepository,
                              MemberInfoRepository memberInfoRepository,
                              AppInfoRepository appInfoRepository) {
        this.accountRepository = accountRepository;
        this.memberInfoRepository = memberInfoRepository;
        this.appInfoRepository = appInfoRepository;
    }

    @Override
    public AccountDTO getById(Long accountId) {
        Optional<SysAccount> account = accountRepository.findByIdAndDeletedFalse(accountId);
        if (!account.isPresent()) {
            return null;
        }
        Optional<MemberInfo> member = memberInfoRepository.findByAccountIdAndDeletedFalse(accountId);
        Optional<AppInfo> app = appInfoRepository.findByAccountIdAndDeletedFalse(accountId);
        return AccountConverter.toDTO(account.get(), member.orElse(null), app.orElse(null));
    }

    // ---------------- openapi 凭证提供者 ----------------

    @Override
    public AppIdentity load(String appKey) {
        return appInfoRepository.findByAppKeyAndDeletedFalse(appKey)
                .map(app -> {
                    if (!accountEnabled(app.getAccountId())) {
                        return null;
                    }
                    AppIdentity identity = new AppIdentity();
                    identity.setAccountId(app.getAccountId());
                    identity.setAppKey(app.getAppKey());
                    identity.setAppSecret(app.getAppSecret());
                    identity.setAppName(app.getAppName());
                    Set<String> scopes = new HashSet<>();
                    if (app.getScopes() != null && !app.getScopes().isEmpty()) {
                        scopes.addAll(Arrays.asList(app.getScopes().split(",")));
                    }
                    identity.setScopes(scopes);
                    return identity;
                })
                .orElse(null);
    }

    private boolean accountEnabled(Long accountId) {
        return accountRepository.findByIdAndDeletedFalse(accountId)
                .map(a -> "ENABLE".equals(a.getStatus())).orElse(false);
    }

    // ---------------- 管理端：三方应用管理 ----------------

    @Transactional
    public AppInfo createApp(String appName, String scopes, String ipWhitelist) {
        SysAccount account = new SysAccount();
        account.setAccountNo("APP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        account.setType(AccountType.APP);
        account = accountRepository.save(account);

        AppInfo app = new AppInfo();
        app.setAccountId(account.getId());
        app.setAppName(appName);
        app.setScopes(scopes);
        app.setIpWhitelist(ipWhitelist);
        app.setAppKey("ak-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        app.setAppSecret("sk-" + UUID.randomUUID().toString().replace("-", ""));
        AppInfo saved = appInfoRepository.save(app);
        log.info("[app] 三方应用创建: appKey={} name={}", saved.getAppKey(), appName);
        return saved;
    }

    @Transactional
    public AppInfo resetSecret(Long appId) {
        AppInfo app = appInfoRepository.findByIdAndDeletedFalse(appId)
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "应用不存在"));
        app.setAppSecret("sk-" + UUID.randomUUID().toString().replace("-", ""));
        return appInfoRepository.save(app);
    }
}
