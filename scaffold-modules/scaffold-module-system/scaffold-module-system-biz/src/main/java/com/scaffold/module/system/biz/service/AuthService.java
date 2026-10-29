package com.scaffold.module.system.biz.service;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.module.system.biz.entity.MemberInfo;
import com.scaffold.module.system.biz.entity.SysAccount;
import com.scaffold.module.system.biz.repository.MemberInfoRepository;
import com.scaffold.module.system.biz.repository.SysAccountRepository;
import com.scaffold.starter.security.identity.AccountType;
import com.scaffold.starter.security.online.ExternalIdentity;
import com.scaffold.starter.security.token.SessionInfo;
import com.scaffold.starter.security.token.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 认证服务：internal 账密登录 / online 微信登录 / 登出——殊途同归到统一账户 + Redis 会话。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SysAccountRepository accountRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final TokenService tokenService;
    private final OperatorUserDetailsService userDetailsService;
    private final OAuthLoginService oAuthLoginService;

    public AuthService(SysAccountRepository accountRepository,
                       MemberInfoRepository memberInfoRepository,
                       TokenService tokenService,
                       OperatorUserDetailsService userDetailsService,
                       OAuthLoginService oAuthLoginService) {
        this.accountRepository = accountRepository;
        this.memberInfoRepository = memberInfoRepository;
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
        this.oAuthLoginService = oAuthLoginService;
    }

    /** internal：账密登录（OPERATOR） */
    @Transactional
    public String internalLogin(String username, String password) {
        SysAccount account = accountRepository.findByAccountNoAndDeletedFalse(username)
                .orElseThrow(() -> BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号或密码错误"));
        if (account.getType() != AccountType.OPERATOR) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "该账号不支持管理端登录");
        }
        if (!"ENABLE".equals(account.getStatus())) {
            throw BizException.of(GlobalErrorCode.FORBIDDEN, "账号已停用");
        }
        if (!userDetailsService.matches(password, account.getPassword())) {
            log.warn("[auth] 密码错误: accountNo={}", username);
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        return issueToken(account, "internal", true);
    }

    /** online：微信授权码登录（MEMBER，不存在则自动注册） */
    @Transactional
    public String wechatLogin(String provider, String code) {
        ExternalIdentity identity = oAuthLoginService.exchange(provider, code);
        if (identity == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "登录源未接入: " + provider);
        }
        MemberInfo member = memberInfoRepository.findByOpenIdAndDeletedFalse(identity.getOpenId())
                .orElseGet(() -> registerMember(identity));
        SysAccount account = accountRepository.findByIdAndDeletedFalse(member.getAccountId())
                .orElseThrow(() -> new IllegalStateException("会员关联账户缺失: " + member.getAccountId()));
        if (!"ENABLE".equals(account.getStatus())) {
            throw BizException.of(GlobalErrorCode.FORBIDDEN, "账号已停用");
        }
        return issueToken(account, "online:" + provider, false);
    }

    private MemberInfo registerMember(ExternalIdentity identity) {
        SysAccount account = new SysAccount();
        account.setAccountNo("M" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        account.setType(AccountType.MEMBER);
        account = accountRepository.save(account);

        MemberInfo member = new MemberInfo();
        member.setAccountId(account.getId());
        member.setOpenId(identity.getOpenId());
        member.setUnionId(identity.getUnionId());
        member.setNickname(identity.getNickname() == null ? "微信用户" : identity.getNickname());
        member.setAvatar(identity.getAvatar());
        memberInfoRepository.save(member);
        log.info("[auth] 新会员注册: accountId={}", account.getId());
        return member;
    }

    private String issueToken(SysAccount account, String device, boolean exclusive) {
        SessionInfo session = new SessionInfo();
        session.setAccountId(account.getId());
        session.setAccountNo(account.getAccountNo());
        session.setAccountType(account.getType().name());
        session.setDevice(device);
        if (account.getType() == AccountType.OPERATOR) {
            session.setRoles(userDetailsService.loadRoles(account.getId()));
            session.setPermissions(userDetailsService.loadPermissions(account.getId()));
        }
        return tokenService.create(session, exclusive);
    }

    /** 登出：删 key 立即失效 */
    public void logout(String token) {
        tokenService.revoke(token);
    }
}
