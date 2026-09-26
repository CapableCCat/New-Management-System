package com.tsguosc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 反馈提交请求（F-014）。
 *
 * <p>**免登录接口** —— 因此必须带一次性图形验证码（与报名 F-001 / 查询 F-005 同源），
 * 否则任何人都能无限灌垃圾。
 *
 * <p>{@code source} 由前端传入（它的用途只是"复盘时区分来源"，不涉及权限），
 * 后端只校验它必须是字典 {@code feedback_source} 里的启用项。
 */
public record FeedbackSubmitRequest(

        @NotBlank(message = "请填写反馈内容")
        @Size(max = 1000, message = "反馈内容不能超过 1000 字")
        String content,

        @Size(max = 64, message = "联系方式不能超过 64 个字符")
        String contact,

        @NotNull(message = "缺少反馈来源")
        @Min(value = 1, message = "反馈来源不合法")
        @Max(value = 2, message = "反馈来源不合法")
        Integer source,

        @NotBlank(message = "请填写验证码")
        String captchaKey,

        @NotBlank(message = "请填写验证码")
        String captchaCode
) {
}
