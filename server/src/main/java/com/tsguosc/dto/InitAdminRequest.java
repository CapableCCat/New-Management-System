package com.tsguosc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建首个超管请求（F-012）。
 */
public record InitAdminRequest(

        @NotBlank(message = "请填写姓名")
        @Size(max = 32, message = "姓名不能超过 32 个字符")
        String name,

        @NotBlank(message = "请填写手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "请填写密码")
        String password
) {
}
