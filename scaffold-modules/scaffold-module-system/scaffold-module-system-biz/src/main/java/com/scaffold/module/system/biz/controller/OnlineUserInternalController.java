package com.scaffold.module.system.biz.controller;

import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.R;
import com.scaffold.common.util.StringUtils;
import com.scaffold.starter.security.permission.RequirePermission;
import com.scaffold.starter.security.token.SessionInfo;
import com.scaffold.starter.security.token.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * internal 端在线用户管理接口。
 *
 * <p>会话数据在 Redis（{@code TokenService} 全量在线查询），非数据库表，
 * 过滤与分页在内存完成——在线会话量级可控，直接可读；
 * 踢人即删会话 key，被踢端下一请求立即 401，无需广播。</p>
 *
 * <p>权限拆两级：查询 {@code system:online:query}、踢人 {@code system:online:manage}——
 * 只读与写操作分开授权，审计类页面常见给查询不给踢。</p>
 *
 * @author lianghang
 */
@Tag(name = "internal-在线用户")
@RestController
@RequestMapping("/internal/system/online")
public class OnlineUserInternalController {

    private final TokenService tokenService;

    public OnlineUserInternalController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /** 在线用户条目：token 作为会话标识随出参返回，供前端踢单个会话时回传。 */
    public static class OnlineUserItem {

        private final String token;
        private final Long accountId;
        private final String accountNo;
        private final String displayName;
        private final String device;
        private final long loginTime;

        OnlineUserItem(String token, SessionInfo session) {
            this.token = token;
            this.accountId = session.getAccountId();
            this.accountNo = session.getAccountNo();
            this.displayName = session.getDisplayName();
            this.device = session.getDevice();
            this.loginTime = session.getLoginTime();
        }

        public String getToken() {
            return token;
        }

        public Long getAccountId() {
            return accountId;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDevice() {
            return device;
        }

        public long getLoginTime() {
            return loginTime;
        }
    }

    @Operation(summary = "在线用户分页（按账号/姓名关键字过滤）")
    @RequirePermission("system:online:query")
    @GetMapping
    public R<PageResult<OnlineUserItem>> list(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "20") int pageSize,
                                              @RequestParam(required = false) String keyword) {
        Map<String, SessionInfo> sessions = tokenService.listAllOnlineSessions();
        List<OnlineUserItem> all = new ArrayList<>(sessions.size());
        for (Map.Entry<String, SessionInfo> entry : sessions.entrySet()) {
            OnlineUserItem item = new OnlineUserItem(entry.getKey(), entry.getValue());
            if (matches(item, keyword)) {
                all.add(item);
            }
        }
        // 登录时间倒序：最近活跃的排前面
        all.sort((a, b) -> Long.compare(b.getLoginTime(), a.getLoginTime()));

        long total = all.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<OnlineUserItem> page = from >= to ? new ArrayList<>() : all.subList(from, to);
        return R.ok(PageResult.of(new ArrayList<>(page), total, pageNum, pageSize));
    }

    @Operation(summary = "踢单个会话下线（token 即列表接口返回的会话标识）")
    @RequirePermission("system:online:manage")
    @PostMapping("/kick")
    public R<Void> kick(@RequestParam String token) {
        tokenService.revoke(token);
        return R.ok();
    }

    private boolean matches(OnlineUserItem item, String keyword) {
        if (StringUtils.isEmpty(keyword)) {
            return true;
        }
        return contains(item.getAccountNo(), keyword) || contains(item.getDisplayName(), keyword);
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
    }
}
