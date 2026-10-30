package com.scaffold.module.system.biz.controller;

import com.scaffold.common.result.R;
import com.scaffold.module.system.api.dto.DictDataDTO;
import com.scaffold.module.system.biz.service.AuthService;
import com.scaffold.module.system.biz.service.DictServiceImpl;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.identity.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * online 端接口。
 *
 * <p>{@code /online/**} 整链 permitAll：匿名可 ping、可微信授权登录、可查公开字典；
 * 登录后 {@code LoginContext} 可用——「匿名可用 + 登录增强」的分层示范。</p>
 *
 * @author lianghang
 */
@Tag(name = "online-线上端")
@RestController
@RequestMapping("/online")
public class OnlineController {

    private final AuthService authService;
    private final DictServiceImpl dictService;

    public OnlineController(AuthService authService, DictServiceImpl dictService) {
        this.authService = authService;
        this.dictService = dictService;
    }

    @Operation(summary = "健康检查（匿名）")
    @GetMapping("/ping")
    public R<String> ping() {
        return R.ok("pong");
    }

    @Operation(summary = "微信授权登录（provider: wechat-mp / wechat-mini）")
    @PostMapping("/auth/login")
    public R<Map<String, Object>> login(@RequestParam String provider, @RequestParam String code) {
        String token = authService.wechatLogin(provider, code);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        return R.ok(result);
    }

    @Operation(summary = "我的信息（登录后可用，匿名返回未登录）")
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        LoginUser user = LoginContext.currentOrNull();
        Map<String, Object> result = new HashMap<>();
        result.put("loggedIn", user != null);
        if (user != null) {
            result.put("accountId", user.getAccountId());
            result.put("displayName", user.getDisplayName());
        }
        return R.ok(result);
    }

    @Operation(summary = "公开字典查询（匿名可用）")
    @GetMapping("/dict")
    public R<List<DictDataDTO>> dict(@RequestParam String type) {
        return R.ok(dictService.listByType(type));
    }
}
