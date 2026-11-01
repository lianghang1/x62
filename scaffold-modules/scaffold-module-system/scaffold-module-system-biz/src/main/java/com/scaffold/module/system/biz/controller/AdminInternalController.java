package com.scaffold.module.system.biz.controller;

import com.scaffold.module.system.api.annotation.OperLog;
import com.scaffold.common.result.R;
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
    private final com.scaffold.module.system.biz.service.JobAdminService jobAdminService;
    private final com.scaffold.module.system.biz.service.RoleAdminService roleAdminService;

    public AdminInternalController(AccountServiceImpl accountService,
                                   AccountAdminService accountAdminService,
                                   OperatorUserDetailsService userDetailsService,
                                   SysRoleRepository roleRepository,
                                   com.scaffold.starter.security.token.TokenService tokenService,
                                   com.scaffold.module.system.biz.service.JobAdminService jobAdminService,
                                   com.scaffold.module.system.biz.service.RoleAdminService roleAdminService) {
        this.accountService = accountService;
        this.accountAdminService = accountAdminService;
        this.userDetailsService = userDetailsService;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
        this.jobAdminService = jobAdminService;
        this.roleAdminService = roleAdminService;
    }

    @Operation(summary = "创建三方应用（返回 appKey/appSecret，secret 仅此一次展示）")
    @OperLog(module = "三方应用", action = "创建应用")
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
    @OperLog(module = "三方应用", action = "重置密钥")
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
    @OperLog(module = "角色", action = "分配角色")
    @PostMapping("/accounts/{accountId}/roles")
    public R<Void> assignRoles(@PathVariable Long accountId,
                               @RequestParam("roleIds") List<Long> roleIds) {
        userDetailsService.assignRoles(accountId, roleIds);
        return R.ok();
    }

    @Operation(summary = "踢人下线")
    @OperLog(module = "在线用户", action = "踢人下线")
    @PostMapping("/accounts/{accountId}/kick")
    public R<Void> kick(@PathVariable Long accountId) {
        tokenService.revokeAll(accountId);
        return R.ok();
    }

    @Operation(summary = "账号分页查询（账号模糊/类型/状态过滤，不含密码）")
    @GetMapping("/accounts")
    public R<com.scaffold.common.result.PageResult<Map<String, Object>>> accounts(
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
    @OperLog(module = "账号管理", action = "创建账号")
    @PostMapping("/accounts")
    public R<Map<String, Object>> createAccount(@RequestParam String accountNo,
                                                @RequestParam String password) {
        return R.ok(accountAdminService.createOperator(accountNo, password));
    }

    @Operation(summary = "启用/停用账号（停用即全端下线）")
    @OperLog(module = "账号管理", action = "变更状态")
    @PostMapping("/accounts/{accountId}/status")
    public R<Void> changeStatus(@PathVariable Long accountId, @RequestParam boolean enable) {
        accountAdminService.changeStatus(accountId, enable);
        return R.ok();
    }

    @Operation(summary = "重置密码（随机密码明文仅此一次返回，并全端下线强制重登）")
    @OperLog(module = "账号管理", action = "重置密码")
    @PostMapping("/accounts/{accountId}/reset-password")
    public R<Map<String, Object>> resetPassword(@PathVariable Long accountId) {
        String rawPassword = accountAdminService.resetPassword(accountId);
        Map<String, Object> result = new HashMap<>();
        result.put("password", rawPassword);
        return R.ok(result);
    }

    // ---------------- 任务管理（simple 引擎） ----------------

    @Operation(summary = "任务列表（注册表快照与表配置合并视图，含孤儿配置）")
    @GetMapping("/jobs")
    public R<List<Map<String, Object>>> jobs() {
        return R.ok(jobAdminService.list());
    }

    @Operation(summary = "保存任务配置（改 cron/启停，落表并动态刷新调度，不重启）")
    @OperLog(module = "任务管理", action = "保存配置")
    @PostMapping("/jobs/{jobKey}/config")
    public R<Void> saveJobConfig(@PathVariable String jobKey,
                                 @RequestParam String cron,
                                 @RequestParam boolean enabled,
                                 @RequestParam(required = false) String remark) {
        jobAdminService.saveConfig(jobKey, cron, enabled, remark);
        return R.ok();
    }

    @Operation(summary = "立即触发一次任务（异步，锁/日志语义与定时触发一致）")
    @OperLog(module = "任务管理", action = "触发任务")
    @PostMapping("/jobs/{jobKey}/trigger")
    public R<Void> triggerJob(@PathVariable String jobKey) {
        jobAdminService.trigger(jobKey);
        return R.ok();
    }

    @Operation(summary = "执行日志分页（jobName 可空 = 全部任务混排，最新在前）")
    @GetMapping("/jobs/run-logs")
    public R<com.scaffold.common.result.PageResult<com.scaffold.starter.job.JobRunLog>> jobRunLogs(
            @RequestParam(required = false) String jobName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(jobAdminService.runLogs(jobName, pageNum, pageSize));
    }

    // ---------------- 角色管理 ----------------

    @Operation(summary = "创建角色（编码唯一）")
    @OperLog(module = "角色", action = "创建角色")
    @PostMapping("/roles")
    public R<SysRole> createRole(@RequestParam String code,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String permissions) {
        return R.ok(roleAdminService.create(code, name, permissions));
    }

    @Operation(summary = "更新角色（名称/权限串，编码不可改）")
    @OperLog(module = "角色", action = "更新角色")
    @PostMapping("/roles/{roleId}/update")
    public R<SysRole> updateRole(@PathVariable Long roleId,
                                 @RequestParam(required = false) String name,
                                 @RequestParam(required = false) String permissions) {
        return R.ok(roleAdminService.update(roleId, name, permissions));
    }

    @Operation(summary = "删除角色（有账号绑定时拒绝）")
    @OperLog(module = "角色", action = "删除角色")
    @PostMapping("/roles/{roleId}/delete")
    public R<Void> deleteRole(@PathVariable Long roleId) {
        roleAdminService.delete(roleId);
        return R.ok();
    }
}
