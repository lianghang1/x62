package com.scaffold.module.system.biz.controller;

import com.scaffold.common.result.R;
import com.scaffold.module.system.api.annotation.OperLog;
import com.scaffold.module.system.api.dto.DictDataDTO;
import com.scaffold.module.system.biz.entity.SysDict;
import com.scaffold.module.system.biz.service.DictServiceImpl;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.identity.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * internal 端系统接口。
 *
 * <p>提供当前登录信息与字典管理：{@code /me} 演示 {@code LoginContext}
 * 的唯一取用入口，字典保存演示 RBAC 注解鉴权——internal 端的最小可用示例。</p>
 *
 * @author lianghang
 */
@Tag(name = "internal-系统")
@RestController
@RequestMapping("/internal/system")
public class SystemInternalController {

    private final DictServiceImpl dictService;

    public SystemInternalController(DictServiceImpl dictService) {
        this.dictService = dictService;
    }

    @Operation(summary = "当前登录用户（LoginContext 唯一入口演示）")
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        LoginUser user = LoginContext.current();
        Map<String, Object> result = new HashMap<>();
        result.put("accountId", user.getAccountId());
        result.put("accountNo", user.getAccountNo());
        result.put("accountType", user.getAccountType());
        result.put("displayName", user.getDisplayName());
        result.put("roles", user.getRoles());
        return R.ok(result);
    }

    @Operation(summary = "新增/修改字典项（需 system:dict:manage 权限）")
    @OperLog(module = "数据字典", action = "保存字典")
    @PostMapping("/dict")
    public R<Long> saveDict(@RequestBody SysDict dict) {
        return R.ok(dictService.save(dict).getId());
    }

    @Operation(summary = "字典查询")
    @GetMapping("/dict")
    public R<DictDataDTO> dict(@RequestParam String type, @RequestParam String value) {
        for (DictDataDTO dto : dictService.listByType(type)) {
            if (dto.getValue().equals(value)) {
                return R.ok(dto);
            }
        }
        return R.ok(null);
    }
}
