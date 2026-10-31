package com.scaffold.starter.security.token;

import com.scaffold.common.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 基于 Redis 的 token 会话实现，沿用 RuoYi 的同款思路。
 *
 * <p>token 为 UUID，会话 JSON 存 {@code scaffold:auth:token:{token}}，TTL 可配；
 * 账户索引 set {@code scaffold:auth:account:{accountId}} 记录其全部 token，供全端下线与在线用户查询使用；
 * 剩余有效期不足阈值自动续期，踢人即删 key、立即生效——双 key 结构让会话管控有索引可依。</p>
 *
 * @author lianghang
 */
public class RedisTokenService implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenService.class);

    private static final String TOKEN_KEY = com.scaffold.common.constant.RedisKeys.AUTH_TOKEN;
    private static final String ACCOUNT_KEY = com.scaffold.common.constant.RedisKeys.AUTH_ACCOUNT;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long ttlSeconds;
    private final int renewThresholdPercent;

    public RedisTokenService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                             long ttlSeconds, int renewThresholdPercent) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
        this.renewThresholdPercent = renewThresholdPercent;
    }

    @Override
    public String create(SessionInfo session, boolean exclusive) {
        if (exclusive) {
            revokeAll(session.getAccountId());
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(TOKEN_KEY + token,
                    objectMapper.writeValueAsString(session), Duration.ofSeconds(ttlSeconds));
            redisTemplate.opsForSet().add(ACCOUNT_KEY + session.getAccountId(), token);
            log.info("[auth] 会话创建: accountId={} device={}", session.getAccountId(), session.getDevice());
        } catch (Exception e) {
            throw new IllegalStateException("会话写入 Redis 失败", e);
        }
        return token;
    }

    @Override
    public SessionInfo resolve(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        String json = redisTemplate.opsForValue().get(TOKEN_KEY + token);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SessionInfo.class);
        } catch (Exception e) {
            log.warn("[auth] 会话反序列化失败，已按无效处理: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public SessionInfo resolveWithRenew(String token) {
        SessionInfo session = resolve(token);
        if (session == null) {
            return null;
        }
        String key = TOKEN_KEY + token;
        Long remain = redisTemplate.getExpire(key);
        if (remain != null && remain > 0 && remain < ttlSeconds * renewThresholdPercent / 100) {
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
            log.debug("[auth] 会话自动续期: accountId={}", session.getAccountId());
        }
        return session;
    }

    @Override
    public void revoke(String token) {
        if (token == null) {
            return;
        }
        SessionInfo session = resolve(token);
        redisTemplate.delete(TOKEN_KEY + token);
        if (session != null) {
            redisTemplate.opsForSet().remove(ACCOUNT_KEY + session.getAccountId(), token);
        }
    }

    @Override
    public void revokeAll(Long accountId) {
        Set<String> tokens = redisTemplate.opsForSet().members(ACCOUNT_KEY + accountId);
        if (tokens != null && !tokens.isEmpty()) {
            List<String> keys = new ArrayList<>(tokens.size());
            for (String t : tokens) {
                keys.add(TOKEN_KEY + t);
            }
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(ACCOUNT_KEY + accountId);
        log.info("[auth] 账户全端下线: accountId={}", accountId);
    }

    @Override
    public List<SessionInfo> onlineSessions(Long accountId) {
        Set<String> tokens = redisTemplate.opsForSet().members(ACCOUNT_KEY + accountId);
        List<SessionInfo> sessions = new ArrayList<>();
        if (tokens == null) {
            return sessions;
        }
        for (String t : tokens) {
            SessionInfo s = resolve(t);
            if (s != null) {
                sessions.add(s);
            } else {
                // 清理过期残留索引
                redisTemplate.opsForSet().remove(ACCOUNT_KEY + accountId, t);
            }
        }
        return sessions;
    }
}
