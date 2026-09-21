package com.tsguosc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量通过请求：部门取各自意向部门的第一个，职位统一指定（默认成员）。
 */
public record RecruitApproveBatchRequest(

        @NotEmpty(message = "请至少选择一条待审记录")
        List<Long> ids,

        @Min(value = 0, message = "职位取值不合法")
        @Max(value = 3, message = "职位取值不合法")
        Integer duty
) {
}
