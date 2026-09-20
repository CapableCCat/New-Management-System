package com.tsguosc.controller;

import com.tsguosc.common.result.Result;
import com.tsguosc.dto.CaptchaVO;
import com.tsguosc.dto.InitAdminRequest;
import com.tsguosc.dto.InitStatusVO;
import com.tsguosc.dto.LoginRequest;
import com.tsguosc.dto.LoginVO;
import com.tsguosc.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与初始化接口（本组接口全部免登录，见 SaTokenConfig 白名单）。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 图形验证码（算术型） */
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(authService.createCaptcha());
    }

    /** 系统是否已初始化；前端据此决定是否强制跳引导页 */
    @GetMapping("/init-status")
    public Result<InitStatusVO> initStatus() {
        return Result.ok(authService.initStatus());
    }

    /** 创建首个超管（仅未初始化时可用） */
    @PostMapping("/init-admin")
    public Result<Void> initAdmin(@Valid @RequestBody InitAdminRequest request) {
        authService.initAdmin(request);
        return Result.ok(null, "超管创建成功，请登录");
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request), "登录成功");
    }

    /** 登出 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok(null, "已退出登录");
    }
}
