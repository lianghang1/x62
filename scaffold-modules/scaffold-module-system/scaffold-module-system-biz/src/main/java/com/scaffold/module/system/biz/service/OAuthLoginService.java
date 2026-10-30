package com.scaffold.module.system.biz.service;

import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.starter.security.online.ExternalIdentity;
import com.scaffold.starter.security.online.OAuthIdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OAuth 登录源路由器。
 *
 * <p>按 provider 标识路由到对应的 {@code OAuthIdentityProvider} 实现
 * （微信网页授权 / 小程序等，按需扩展）：启动时收集全部实现建立路由表，
 * 一个都没有则只告警——{@code /online} 登录不可用，其余入口不受影响；
 * 未知 provider 直接拒绝，不猜测默认登录源。</p>
 *
 * @author lianghang
 */
@Service
public class OAuthLoginService {

    private static final Logger log = LoggerFactory.getLogger(OAuthLoginService.class);

    private final Map<String, OAuthIdentityProvider> providers;

    public OAuthLoginService(List<OAuthIdentityProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(OAuthIdentityProvider::provider, p -> p));
        if (providers.isEmpty()) {
            log.warn("[auth] 未接入任何 OAuth 登录源（/online 登录不可用，其余入口不受影响）");
        }
    }

    /**
     * 用授权码换取外部身份。
     *
     * @param provider 登录源标识
     * @param code     授权码
     * @return 外部身份信息
     */
    public ExternalIdentity exchange(String provider, String code) {
        OAuthIdentityProvider p = providers.get(provider);
        if (p == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "未接入登录源: " + provider);
        }
        return p.exchange(code);
    }
}
