package com.scaffold.starter.security.online;

/**
 * 外部身份（微信等）换取统一账户的 SPI。
 * starter-security 只定义契约；微信生态实现在 module-system-biz（网页授权/小程序 code2session）。
 * 未配置任何实现时，/online 的登录接口返回明确的「未接入登录源」业务错误。
 */
public interface OAuthIdentityProvider {

    /** 身份源标识：wechat-mp（公众号网页授权）/ wechat-mini（小程序）… */
    String provider();

    /**
     * 用授权码换取外部身份。
     *
     * @throws com.scaffold.core.exception.BizException 换取失败（code 无效等）
     */
    ExternalIdentity exchange(String code);
}
