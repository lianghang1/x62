package com.scaffold.starter.security.filter;

import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.starter.security.config.SecurityProperties;
import com.scaffold.starter.security.openapi.AppIdentity;
import com.scaffold.starter.security.openapi.AppSecretProvider;
import com.scaffold.starter.security.openapi.SignUtils;
import com.scaffold.starter.security.identity.AccountType;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.identity.LoginUser;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * openapi 链的 HMAC 签名认证过滤器。
 *
 * <p>签名要素为 appKey + timestamp + nonce + body 摘要；防重放依赖时间戳窗口
 * 加 nonce 一次性（Redis setIfAbsent）。fail closed：未配置凭证提供者时全部拒绝，
 * 多文件请求（multipart）不参与签名直接放行——机器对机器的调用走签名而非会话。</p>
 *
 * @author lianghang
 */
public class OpenApiSignFilter extends OncePerRequestFilter {

    public static final String HEADER_APP_KEY = "X-App-Key";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGN = "X-Sign";

    private static final String NONCE_KEY = "scaffold:openapi:nonce:";

    private final AppSecretProvider appSecretProvider;
    private final SecurityProperties properties;
    private final StringRedisTemplate redisTemplate;

    public OpenApiSignFilter(AppSecretProvider appSecretProvider,
                             SecurityProperties properties,
                             StringRedisTemplate redisTemplate) {
        this.appSecretProvider = appSecretProvider;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return properties.getOpenapiWhitelist().contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            if (request.getContentType() != null
                    && request.getContentType().toLowerCase().contains("multipart")) {
                // 文件上传不参与签名，直接放行（如需保护可按业务扩展）
                chain.doFilter(request, response);
                return;
            }
            CachedBodyRequestWrapper wrapped = new CachedBodyRequestWrapper(request);
            if (authenticate(wrapped, response)) {
                chain.doFilter(wrapped, response);
            }
        } finally {
            AuthenticationSupport.clear();
        }
    }

    private boolean authenticate(CachedBodyRequestWrapper request,
                                 HttpServletResponse response) throws IOException {
        String appKey = request.getHeader(HEADER_APP_KEY);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String sign = request.getHeader(HEADER_SIGN);

        if (anyBlank(appKey, timestamp, nonce, sign)) {
            return reject(response, GlobalErrorCode.UNAUTHORIZED);
        }
        // 1. 应用存在性
        AppIdentity app = appSecretProvider.load(appKey);
        if (app == null) {
            return reject(response, GlobalErrorCode.UNAUTHORIZED);
        }
        // 2. 时间戳窗口
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            return reject(response, GlobalErrorCode.UNAUTHORIZED);
        }
        long windowMillis = properties.getOpenapiTimestampWindowSeconds() * 1000L;
        if (Math.abs(System.currentTimeMillis() - ts) > windowMillis) {
            return reject(response, GlobalErrorCode.UNAUTHORIZED);
        }
        // 3. nonce 一次性
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(NONCE_KEY + nonce, "1", windowMillis, TimeUnit.MILLISECONDS);
        if (first == null || !first) {
            return reject(response, GlobalErrorCode.CONFLICT);
        }
        // 4. 签名校验
        String body = request.getBodyString();
        String signBase = appKey + "\n" + timestamp + "\n" + nonce + "\n"
                + SignUtils.sha256Hex(body == null ? "" : body);
        String expected = SignUtils.hmacSha256Hex(signBase, app.getAppSecret());
        if (!SignUtils.constantTimeEquals(expected, sign)) {
            return reject(response, GlobalErrorCode.UNAUTHORIZED);
        }

        // 5. 归一化为 APP 账户上下文
        LoginUser user = new LoginUser();
        user.setAccountId(app.getAccountId());
        user.setAccountNo(appKey);
        user.setAccountType(AccountType.APP);
        user.setDisplayName(app.getAppName());
        user.setPermissions(app.getScopes() == null ? Collections.<String>emptySet() : app.getScopes());
        user.setDevice("openapi:" + appKey);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (app.getScopes() != null) {
            for (String scope : app.getScopes()) {
                authorities.add(new SimpleGrantedAuthority(scope));
            }
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        LoginContext.set(user);
        return true;
    }

    private boolean anyBlank(String... values) {
        for (String v : values) {
            if (v == null || v.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean reject(HttpServletResponse response, GlobalErrorCode errorCode) throws IOException {
        AuthenticationSupport.writeJson(response, errorCode);
        return false;
    }
}
