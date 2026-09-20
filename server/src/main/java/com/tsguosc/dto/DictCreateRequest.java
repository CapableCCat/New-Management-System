package com.tsguosc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增字典条目请求。
 */
public record DictCreateRequest(

        @NotBlank(message = "请指定字典类型")
        String type,

        @NotBlank(message = "请填写编码")
        @Size(max = 32, message = "编码不能超过 32 个字符")
        String code,

        @NotBlank(message = "请填写文案")
        @Size(max = 64, message = "文案不能超过 64 个字符")
        String label,

        /** 留空则自动追加到该类型末尾 */
        Integer sort,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
