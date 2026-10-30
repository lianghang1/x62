package com.scaffold.module.system.biz.service;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.module.system.biz.entity.LoginLog;
import com.scaffold.module.system.biz.entity.MemberInfo;
import com.scaffold.module.system.biz.entity.SysAccount;
import com.scaffold.module.system.biz.repository.LoginLogRepository;
import com.scaffold.module.system.biz.repository.MemberInfoRepository;
import com.scaffold.module.system.biz.repository.SysAccountRepository;
import com.scaffold.starter.security.identity.AccountType;
import com.scaffold.starter.security.online.ExternalIdentity;
import com.scaffold.starter.security.policy.LoginFailureGuard;
import com.scaffold.starter.security.token.SessionInfo;
import com.scaffold.starter.security.token.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证服务：internal 账密登录 / online 微信登录 / 登出——殊途同归到统一账户 + Redis 会话。
 * 登录全程写 sys_login_log（成功/失败都记），失败走 LoginFailureGuard 连续锁定。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SysAccountRepository accountRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final LoginLogRepository loginLogRepository;
    private final TokenService tokenService;
    private final LoginFailureGuard failureGuard;
    private final OperatorUserDetailsService userDetailsService;
    private final OAuthLoginService oAuthLoginService;

    public AuthService(SysAccountRepository accountRepository,
                       MemberInfoRepository memberInfoRepository,
                       LoginLogRepository loginLogRepository,
                       TokenService tokenService,
                       LoginFailureGuard failureGuard,
                       OperatorUserDetailsService userDetailsService,
                       OAuthLoginService oAuthLoginService) {
        this.accountRepository = accountRepository;
        this.memberInfoRepository = memberInfoRepository;
        this.loginLogRepository = loginLogRepository;
        this.tokenService = tokenService;
        this.failureGuard = failureGuard;
        this.userDetailsService = userDetailsService;
        this.oAuthLoginService = oAuthLoginService;
    }

    /** internal：账密登录（OPERATOR）+ 失败锁定 + 登录日志 */
    public String internalLogin(String username, String password) {
        if (failureGuard.isLocked(username)) {
            recordLoginLog(username, "internal", false, "连续失败临时锁定");
            throw BizException.of(GlobalErrorCode.FORBIDDEN,
                    "连续登录失败次数过多，账号已临时锁定，请稍后再试");
        }
        SysAccount account = accountRepository.findByAccountNoAndDeletedFalse(username).orElse(null);
        if (account == null || account.getType() != AccountType.OPERATOR) {
            recordFailureAndLog(username, "internal", "账号不存在或不支持管理端登录");
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (!"ENABLE".equals(account.getStatus())) {
            recordLoginLog(username, "internal", false, "账号已停用");
            throw BizException.of(GlobalErrorCode.FORBIDDEN, "账号已停用");
        }
        if (!userDetailsService.matches(password, account.getPassword())) {
            recordFailureAndLog(username, "internal", "密码错误");
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        failureGuard.reset(username);
        recordLoginLog(username, "internal", true, null);
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
        recordLoginLog(account.getAccountNo(), "online:" + provider, true, null);
        return issueToken(account, "online:" + provider, false);
    }

    private void recordFailureAndLog(String username, String loginType, String reason) {
        int count = failureGuard.recordFailure(username);
        log.warn("[auth] 登录失败({} 次): accountNo={}, 原因={}", count, username, reason);
        recordLoginLog(username, loginType, false, reason);
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

    /** 登录日志：尽力而为，记录失败不影响登录主流程 */
    private void recordLoginLog(String accountNo, String loginType, boolean success, String failReason) {
        try {
            LoginLog entry = new LoginLog();
            entry.setAccountNo(accountNo);
            entry.setLoginType(loginType);
            entry.setSuccess(success);
            entry.setFailReason(failReason);
            entry.setLoginTime(LocalDateTime.now());
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                entry.setIp(clientIp(request));
                String ua = request.getHeader("User-Agent");
                entry.setUserAgent(ua != null && ua.length() > 255 ? ua.substring(0, 255) : ua);
            }
            loginLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("[auth] 登录日志记录失败", e);
        }
    }

    /** 取真实 IP：优先 X-Forwarded-For 首段（经网关/代理），其次 remoteAddr（OperLogAspect 复用） */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
