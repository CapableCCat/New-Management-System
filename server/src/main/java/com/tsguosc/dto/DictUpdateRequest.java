package com.tsguosc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 编辑字典条目请求。
 *
 * <p>注意：**type 与 code 都不可通过本接口修改**（见《开发任务点清单》§6 D19），
 * 因此入参里根本没有这两个字段。
 */
public record DictUpdateRequest(

        @NotBlank(message = "请填写文案")
        @Size(max = 64, message = "文案不能超过 64 个字符")
        String label,

        Integer sort,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark,

        @Min(value = 0, message = "启用状态只能是 0 或 1")
        @Max(value = 1, message = "启用状态只能是 0 或 1")
        Integer enabled
) {
}
