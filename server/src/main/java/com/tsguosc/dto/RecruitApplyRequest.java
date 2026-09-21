package com.tsguosc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 公开报名提交请求（F-001）。
 *
 * <p>必填：姓名、手机号、学院、专业、意向部门（≥1）、验证码；
 * 选填：专业手填值、兴趣标签（≤3）、性别、生源地。
 */
public record RecruitApplyRequest(

        @NotBlank(message = "请填写姓名")
        @Size(max = 32, message = "姓名不能超过 32 个字符")
        String name,

        @NotBlank(message = "请填写手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "请选择学院")
        String college,

        @NotBlank(message = "请选择专业")
        String major,

        @Size(max = 64, message = "专业名称不能超过 64 个字符")
        String majorText,

        @NotEmpty(message = "请至少选择一个意向部门")
        List<String> intentDepartments,

        @Size(max = 3, message = "兴趣标签最多选 3 个")
        List<String> tags,

        /** 兴趣标签选「其他（自由补充）」时的手填值 */
        @Size(max = 64, message = "补充标签不能超过 64 个字符")
        String tagText,

        @Min(value = 0, message = "性别取值不合法")
        @Max(value = 2, message = "性别取值不合法")
        Integer gender,

        @Size(max = 32, message = "省份名称过长")
        String province,

        @Size(max = 32, message = "城市名称过长")
        String city,

        @NotBlank(message = "请填写验证码")
        String captchaKey,

        @NotBlank(message = "请填写验证码")
        String captchaCode
) {
}
