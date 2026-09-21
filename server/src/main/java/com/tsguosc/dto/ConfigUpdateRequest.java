package com.tsguosc.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新系统配置请求（管理端，逐键更新）。
 */
public record ConfigUpdateRequest(

        @NotBlank(message = "请指定配置键")
        String key,

        /** 配置值（不允许空：避免把报名页文案误清空） */
        @NotBlank(message = "配置值不能为空")
        String value
) {
}
