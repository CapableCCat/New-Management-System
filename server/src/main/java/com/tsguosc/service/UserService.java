package com.tsguosc.service;

import com.tsguosc.dto.ChangePasswordRequest;
import com.tsguosc.dto.UserVO;

/**
 * 当前用户相关（F-002 首登改密 / F-007 个人中心 / F-006 成员档案 都会用到）。
 */
public interface UserService {

    /** 当前登录用户信息 */
    UserVO current();

    /** 修改密码：校验旧密码 → 校验新密码强度 → 更新 → 强制登出 */
    void changePassword(ChangePasswordRequest request);
}
