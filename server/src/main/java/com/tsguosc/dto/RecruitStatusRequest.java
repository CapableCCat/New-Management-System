package com.tsguosc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 审核状态查询请求（F-005，公开）。
 *
 * <p>只凭手机号 + 图形验证码查询，不要求登录；手机号放在请求体（不用 GET 查询串），
 * 避免落进 Nginx access log 与浏览器历史。
 */
public record RecruitStatusRequest(

        @NotBlank(message = "请填写手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "请填写验证码")
        String captchaKey,

        @NotBlank(message = "请填写验证码")
        String captchaCode
) {
}
