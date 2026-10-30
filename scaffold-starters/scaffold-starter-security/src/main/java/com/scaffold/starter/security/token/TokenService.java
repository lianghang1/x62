package com.scaffold.starter.security.token;

import java.util.List;

/**
 * 基于「不透明 token + Redis 会话」的 token 会话服务。
 *
 * <p>能力清单借鉴 RuoYi/Sa-Token 的会话管控：创建会话、解析、剩余有效期不足自动续期、
 * 登出/踢人即删 key、按账户全端下线、在线用户查询、同端互斥——服务端持有全部会话状态。</p>
 *
 * @author lianghang
 */
public interface TokenService {

    /**
     * 创建会话并返回 token。
     *
     * @param session      会话内容
     * @param exclusive    同端互斥：true 时该账户同 device 的旧会话全部下线
     */
    String create(SessionInfo session, boolean exclusive);

    /** 解析 token；无效/已过期返回 null（过期自动清理） */
    SessionInfo resolve(String token);

    /** 解析并在剩余有效期不足 renewThresholdPercent 时自动续期 */
    SessionInfo resolveWithRenew(String token);

    /** 登出（删除该 token 会话） */
    void revoke(String token);

    /** 踢人：使某账户所有会话立即失效（改密/封禁场景） */
    void revokeAll(Long accountId);

    /** 某账户剩余有效 token（在线用户管理用） */
    List<SessionInfo> onlineSessions(Long accountId);
}
