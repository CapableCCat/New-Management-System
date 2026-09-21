package com.tsguosc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 审核通过请求（单条）：指定建号时的实际部门与职位。
 *
 * @param id         报名记录 id
 * @param department 实际部门 code（为空时取该报名意向部门的第一个）
 * @param duty       职位 0成员 1副部长 2部长 3社长（为空默认 0 成员）
 */
public record RecruitApproveRequest(

        @NotNull(message = "缺少报名记录 id")
        Long id,

        String department,

        @Min(value = 0, message = "职位取值不合法")
        @Max(value = 3, message = "职位取值不合法")
        Integer duty
) {
}
