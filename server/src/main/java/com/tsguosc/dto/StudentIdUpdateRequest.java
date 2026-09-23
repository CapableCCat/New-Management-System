package com.tsguosc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 学号自助补录请求（PRD F-007）。
 *
 * <p>约束「仅空可补录」：已有学号的账号再调本接口会被告知联系管理员 ——
 * 学号是评优/对接学校系统的验证依据，不允许反复自助改动（§6 D77）。
 */
public record StudentIdUpdateRequest(

        @NotBlank(message = "请填写学号")
        @Size(max = 32, message = "学号不能超过 32 个字符")
        String studentId
) {
}
