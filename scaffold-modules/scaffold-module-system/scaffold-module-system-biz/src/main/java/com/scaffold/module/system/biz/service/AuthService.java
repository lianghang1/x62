package com.scaffold.module.system.biz.service;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.core.util.IpUtils;
import com.scaffold.module.system.biz.entity.LoginLog;
import com.scaffold.module.system.biz.entity.MemberInfo;
import com.scaffold.module.system.biz.entity.SysAccount;
import com.scaffold.module.system.biz.repository.LoginLogRepository;
import com.scaffold.module.system.biz.repository.MemberInfoRepository;
import com.scaffold.module.system.biz.repository.SysAccountRepository;
import com.scaffold.starter.security.identity.AccountType;
import com.scaffold.starter.security.captcha.CaptchaService;
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
 * 认证服务。
 *
 * <p>internal 账密登录 / online 微信登录 / 登出殊途同归到统一账户 + Redis 会话：
 * 登录全程写 {@code sys_login_log}（成功、失败都记），连续失败经
 * {@code LoginFailureGuard} 临时锁定——审计与防护都围绕「身份进出」这条主轴。</p>
 *
 * @author lianghang
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
    private final CaptchaService captchaService;
    private final com.scaffold.starter.security.policy.PasswordPolicyValidator passwordPolicyValidator;

    public AuthService(SysAccountRepository accountRepository,
                       MemberInfoRepository memberInfoRepository,
                       LoginLogRepository loginLogRepository,
                       TokenService tokenService,
                       LoginFailureGuard failureGuard,
                       OperatorUserDetailsService userDetailsService,
                       OAuthLoginService oAuthLoginService,
                       CaptchaService captchaService,
                       com.scaffold.starter.security.policy.PasswordPolicyValidator passwordPolicyValidator) {
        this.accountRepository = accountRepository;
        this.memberInfoRepository = memberInfoRepository;
        this.loginLogRepository = loginLogRepository;
        this.tokenService = tokenService;
        this.failureGuard = failureGuard;
        this.userDetailsService = userDetailsService;
        this.oAuthLoginService = oAuthLoginService;
        this.captchaService = captchaService;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /**
     * internal：账密登录（OPERATOR）+ 失败锁定（账号/IP 双维度）+ 图形验证码 + 登录日志。
     *
     * <p>防刷顺序：IP 撞库防护 → 账号锁定 → 验证码（按策略触发）→ 凭证校验。
     * 任何失败都同时累计账号与 IP 两个维度的失败计数。</p>
     *
     * @param username    账号
     * @param password    明文密码
     * @param captchaId   图形验证码 ID（策略要求时必填）
     * @param captchaCode 图形验证码答案（策略要求时必填）
     * @return token
     */
    public String internalLogin(String username, String password, String captchaId, String captchaCode) {
        String ip = currentIp();
        if (failureGuard.isIpBlocked(ip)) {
            recordLoginLog(username, "internal", false, "IP 登录失败过多被拒");
            throw BizException.of(GlobalErrorCode.TOO_MANY_REQUESTS,
                    "该网络环境登录失败次数过多，请稍后再试");
        }
        if (failureGuard.isLocked(username)) {
            recordLoginLog(username, "internal", false, "连续失败临时锁定");
            throw BizException.of(GlobalErrorCode.FORBIDDEN,
                    "连续登录失败次数过多，账号已临时锁定，请稍后再试");
        }
        if (captchaService.required(failureGuard.currentCount(username))
                && !captchaService.verifyAndConsume(captchaId, captchaCode)) {
            recordFailureAndLog(username, "internal", "验证码错误或已过期");
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "验证码错误或已过期");
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

    /** online：微信授权码登录（MEMBER，不存在则自动注册）。 */
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
        failureGuard.recordIpFailure(currentIp());
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

    /** 登出：删除会话 key，令牌立即失效。 */
    public void logout(String token) {
        tokenService.revoke(token);
    }

    /**
     * 修改自己的密码：验旧密 + 新密强度校验，改密后全端下线强制重登。
     *
     * @param token       当前登录 token
     * @param oldPassword 旧密码明文
     * @param newPassword 新密码明文
     */
    @Transactional
    public void changeMyPassword(String token, String oldPassword, String newPassword) {
        com.scaffold.starter.security.token.SessionInfo session = tokenService.resolve(token);
        if (session == null) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "未登录或凭证无效");
        }
        SysAccount account = accountRepository.findByIdAndDeletedFalse(session.getAccountId())
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "账号不存在"));
        if (!userDetailsService.matches(oldPassword, account.getPassword())) {
            recordLoginLog(session.getAccountNo(), "change-password", false, "旧密码错误");
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "旧密码错误");
        }
        passwordPolicyValidator.validate(newPassword);
        account.setPassword(userDetailsService.encode(newPassword));
        accountRepository.save(account);
        // 改密即全端下线：所有会话（含当前）作废，凭新密码重新登录
        tokenService.revokeAll(session.getAccountId());
        recordLoginLog(session.getAccountNo(), "change-password", true, null);
        log.info("[auth] 密码修改成功: accountId={}", session.getAccountId());
    }

    /** 登录日志：尽力而为，记录失败不影响登录主流程。 */
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

    /** 取真实 IP：委托 core 的 IpUtils（XFF 首段 → X-Real-IP → remoteAddr），OperLogAspect 复用。 */
    public static String clientIp(HttpServletRequest request) {
        return IpUtils.clientIp(request);
    }

    /** 当前请求客户端 IP；无 HTTP 上下文（如单测直调）返回 null，IP 维度防刷自动跳过。 */
    private String currentIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : IpUtils.clientIp(attributes.getRequest());
    }
}
