package com.tsguosc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 反馈处理状态变更请求（PRD 只要求"可查看列表"，标记已处理是复盘辅助，见清单 §6 D123）。
 */
public record FeedbackHandleRequest(

        @NotNull(message = "缺少反馈 id")
        Long id,

        @NotNull(message = "缺少处理状态")
        @Min(value = 0, message = "处理状态不合法")
        @Max(value = 1, message = "处理状态不合法")
        Integer handled
) {
}
