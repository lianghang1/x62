package com.scaffold.module.system.biz.service;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.starter.security.online.ExternalIdentity;
import com.scaffold.starter.security.online.OAuthIdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 登录源路由：provider 标识 -> OAuthIdentityProvider 实现（微信网页授权/小程序等，按需扩展）。
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

    public ExternalIdentity exchange(String provider, String code) {
        OAuthIdentityProvider p = providers.get(provider);
        if (p == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "未接入登录源: " + provider);
        }
        return p.exchange(code);
    }
}
