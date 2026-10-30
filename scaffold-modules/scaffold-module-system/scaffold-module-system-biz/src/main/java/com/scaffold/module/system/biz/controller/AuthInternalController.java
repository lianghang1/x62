package com.scaffold.module.system.biz.controller;

import com.scaffold.common.result.R;
import com.scaffold.module.system.biz.service.AuthService;
import com.scaffold.starter.security.captcha.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * internal 端认证接口。
 *
 * <p>{@code /internal/auth/login} 与 {@code /internal/auth/captcha} 位于匿名白名单，
 * 其余请求需携带 token；登录支持图形验证码（按 {@code scaffold.security.captcha.mode}
 * 策略触发，触发时 captchaId/captchaCode 必填）；登出同时支持
 * {@code Authorization: Bearer} 与 {@code X-Token} 两种取参方式。</p>
 *
 * @author lianghang
 */
@Tag(name = "internal-认证")
@RestController
@RequestMapping("/internal/auth")
public class AuthInternalController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthInternalController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @Operation(summary = "账密登录（验证码策略触发时需携带 captchaId/captchaCode）")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestParam String username,
                                        @RequestParam String password,
                                        @RequestParam(required = false) String captchaId,
                                        @RequestParam(required = false) String captchaCode) {
        String token = authService.internalLogin(username, password, captchaId, captchaCode);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        return R.ok(result);
    }

    @Operation(summary = "图形验证码（base64 图片 + captchaId，一次性）")
    @GetMapping("/captcha")
    public R<Map<String, String>> captcha() {
        return R.ok(captchaService.generate());
    }

    @Operation(summary = "修改自己的密码（验旧密 + 强度校验 + 成功后踢掉本人全部会话）")
    @PostMapping("/password")
    public R<Void> changePassword(@RequestParam String oldPassword,
                                  @RequestParam String newPassword,
                                  HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ")
                ? header.substring(7) : request.getHeader("X-Token");
        authService.changeMyPassword(token, oldPassword, newPassword);
        return R.ok();
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
