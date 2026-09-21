package com.tsguosc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 审核拒绝请求：原因必填（PRD F-003 要求可对外措辞）。
 */
public record RecruitRejectRequest(

        @NotNull(message = "缺少报名记录 id")
        Long id,

        @NotBlank(message = "请填写拒绝原因")
        @Size(max = 255, message = "拒绝原因不能超过 255 个字符")
        String reason
) {
}
