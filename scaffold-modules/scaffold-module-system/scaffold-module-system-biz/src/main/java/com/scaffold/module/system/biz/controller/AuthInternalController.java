package com.scaffold.module.system.biz.controller;

import com.scaffold.core.result.R;
import com.scaffold.module.system.biz.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * internal 端认证（/internal/auth/login 在白名单内，其余需 token）。
 */
@Tag(name = "internal-认证")
@RestController
@RequestMapping("/internal/auth")
public class AuthInternalController {

    private final AuthService authService;

    public AuthInternalController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "账密登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestParam String username, @RequestParam String password) {
        String token = authService.internalLogin(username, password);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        return R.ok(result);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ")
                ? header.substring(7) : request.getHeader("X-Token");
        authService.logout(token);
        return R.ok();
    }
}
