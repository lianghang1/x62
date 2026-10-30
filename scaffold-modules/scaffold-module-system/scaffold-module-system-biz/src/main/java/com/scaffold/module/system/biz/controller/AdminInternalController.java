package com.scaffold.module.system.biz.controller;

import com.scaffold.core.result.R;
import com.scaffold.module.system.biz.entity.AppInfo;
import com.scaffold.module.system.biz.service.AccountAdminService;
import com.scaffold.module.system.biz.service.AccountServiceImpl;
import com.scaffold.module.system.biz.service.OperatorUserDetailsService;
import com.scaffold.module.system.biz.entity.SysRole;
import com.scaffold.module.system.biz.repository.SysRoleRepository;
import com.scaffold.starter.protection.idempotent.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * internal 端管理接口。
 *
 * <p>聚合三方应用、角色与在线用户三类管理动作：写操作全部标注
 * {@code @OperLog} 留痕，创建应用叠加幂等防护——管理动作必须可审计、可重放区分。</p>
 *
 * @author lianghang
 */
@Tag(name = "internal-应用与角色")
@RestController
@RequestMapping("/internal")
public class AdminInternalController {

    private final AccountServiceImpl accountService;
    private final AccountAdminService accountAdminService;
    private final OperatorUserDetailsService userDetailsService;
    private final SysRoleRepository roleRepository;
    private final com.scaffold.starter.security.token.TokenService tokenService;
    private final com.scaffold.starter.security.identity.LoginContext loginContextRef;

    public AdminInternalController(AccountServiceImpl accountService,
                                   AccountAdminService accountAdminService,
                                   OperatorUserDetailsService userDetailsService,
                                   SysRoleRepository roleRepository,
                                   com.scaffold.starter.security.token.TokenService tokenService) {
        this.accountService = accountService;
        this.accountAdminService = accountAdminService;
        this.userDetailsService = userDetailsService;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
        this.loginContextRef = null; // LoginContext 为静态工具，此处仅占位说明
    }

    @Operation(summary = "创建三方应用（返回 appKey/appSecret，secret 仅此一次展示）")
    @com.scaffold.module.system.api.annotation.OperLog(module = "三方应用", action = "创建应用")
    @Idempotent(windowSeconds = 5)
    @PostMapping("/apps")
    public R<Map<String, Object>> createApp(@RequestParam String appName,
                                            @RequestParam(required = false) String scopes,
                                            @RequestParam(required = false) String ipWhitelist) {
        AppInfo app = accountService.createApp(appName, scopes, ipWhitelist);
        Map<String, Object> result = new HashMap<>();
        result.put("id", app.getId());
        result.put("appKey", app.getAppKey());
        result.put("appSecret", app.getAppSecret());
        return R.ok(result);
    }

    @Operation(summary = "重置三方应用密钥")
    @com.scaffold.module.system.api.annotation.OperLog(module = "三方应用", action = "重置密钥")
    @PostMapping("/apps/{id}/reset-secret")
    public R<Map<String, Object>> resetSecret(@PathVariable Long id) {
        AppInfo app = accountService.resetSecret(id);
        Map<String, Object> result = new HashMap<>();
        result.put("appKey", app.getAppKey());
        result.put("appSecret", app.getAppSecret());
        return R.ok(result);
    }

    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public R<List<Map<String, Object>>> roles() {
        return R.ok(StreamSupport.stream(roleRepository.findAll().spliterator(), false)
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", r.getId());
                    m.put("code", r.getCode());
                    m.put("name", r.getName());
                    m.put("permissions", r.getPermissions());
                    return m;
                }).collect(Collectors.toList()));
    }

    @Operation(summary = "给操作员分配角色")
    @com.scaffold.module.system.api.annotation.OperLog(module = "角色", action = "分配角色")
    @PostMapping("/accounts/{accountId}/roles")
    public R<Void> assignRoles(@PathVariable Long accountId,
                               @RequestParam("roleIds") List<Long> roleIds) {
        userDetailsService.assignRoles(accountId, roleIds);
        return R.ok();
    }

    @Operation(summary = "踢人下线")
    @com.scaffold.module.system.api.annotation.OperLog(module = "在线用户", action = "踢人下线")
    @PostMapping("/accounts/{accountId}/kick")
    public R<Void> kick(@PathVariable Long accountId) {
        tokenService.revokeAll(accountId);
        return R.ok();
    }

    @Operation(summary = "账号分页查询（账号模糊/类型/状态过滤，不含密码）")
    @GetMapping("/accounts")
    public R<com.scaffold.core.result.PageResult<Map<String, Object>>> accounts(
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        com.scaffold.starter.security.identity.AccountType accountType = null;
        if (type != null && !type.isEmpty()) {
            accountType = com.scaffold.starter.security.identity.AccountType.valueOf(type);
        }
        return R.ok(accountAdminService.page(accountNo, accountType, status, pageNum, pageSize));
    }

    @Operation(summary = "创建操作员账号（密码强度校验 + BCrypt 落库）")
    @com.scaffold.module.system.api.annotation.OperLog(module = "账号管理", action = "创建账号")
    @PostMapping("/accounts")
    public R<Map<String, Object>> createAccount(@RequestParam String accountNo,
                                                @RequestParam String password) {
        return R.ok(accountAdminService.createOperator(accountNo, password));
    }

    @Operation(summary = "启用/停用账号（停用即全端下线）")
    @com.scaffold.module.system.api.annotation.OperLog(module = "账号管理", action = "变更状态")
    @PostMapping("/accounts/{accountId}/status")
    public R<Void> changeStatus(@PathVariable Long accountId, @RequestParam boolean enable) {
        accountAdminService.changeStatus(accountId, enable);
        return R.ok();
    }

    @Operation(summary = "重置密码（随机密码明文仅此一次返回，并全端下线强制重登）")
    @com.scaffold.module.system.api.annotation.OperLog(module = "账号管理", action = "重置密码")
    @PostMapping("/accounts/{accountId}/reset-password")
    public R<Map<String, Object>> resetPassword(@PathVariable Long accountId) {
        String rawPassword = accountAdminService.resetPassword(accountId);
        Map<String, Object> result = new HashMap<>();
        result.put("password", rawPassword);
        return R.ok(result);
    }
}
