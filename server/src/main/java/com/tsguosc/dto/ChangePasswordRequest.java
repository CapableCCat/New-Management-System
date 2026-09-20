package com.tsguosc.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 修改密码请求（首登强制改密 F-002 / 个人中心 F-007 共用）。
 */
public record ChangePasswordRequest(

        @NotBlank(message = "请填写旧密码")
        String oldPassword,

        @NotBlank(message = "请填写新密码")
        String newPassword
) {
}
