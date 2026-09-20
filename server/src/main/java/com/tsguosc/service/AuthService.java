package com.tsguosc.service;

import com.tsguosc.dto.CaptchaVO;
import com.tsguosc.dto.InitAdminRequest;
import com.tsguosc.dto.InitStatusVO;
import com.tsguosc.dto.LoginRequest;
import com.tsguosc.dto.LoginVO;

/**
 * 认证与初始化（F-002 登录 / F-012 首个超管初始化）。
 */
public interface AuthService {

    /** 生成算术图形验证码（答案存 Redis，校验后作废） */
    CaptchaVO createCaptcha();

    /** 系统是否已初始化（user 表是否有记录） */
    InitStatusVO initStatus();

    /** 创建首个超管（仅在未初始化时可用） */
    void initAdmin(InitAdminRequest request);

    /** 手机号 + 密码 + 验证码登录 */
    LoginVO login(LoginRequest request);

    /** 登出当前登录态 */
    void logout();
}
