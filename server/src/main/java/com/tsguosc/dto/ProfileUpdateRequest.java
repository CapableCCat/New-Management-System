package com.tsguosc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 个人中心「编辑资料」请求（PRD F-007）。
 *
 * <p>可自助修改：学院 / 专业 / 性别 / 生源地 / 个人简介。
 * **姓名与手机号不在请求体里** —— 从根上就不可能被自助改动（PRD 要求需联系管理员）；
 * 学号走单独的补录接口（`/user/student-id`），因为它有「仅空可补录」的约束。
 */
public record ProfileUpdateRequest(

        /** 学院 code；可空（未填写） */
        String college,

        /** 专业 code；可空 */
        String major,

        /** 专业选「其他」时的手填值 */
        @Size(max = 64, message = "专业名称不能超过 64 个字符")
        String majorText,

        @Min(value = 0, message = "性别取值不合法")
        @Max(value = 2, message = "性别取值不合法")
        Integer gender,

        @Size(max = 32, message = "省份名称过长")
        String province,

        @Size(max = 32, message = "城市名称过长")
        String city,

        /** 个人简介（T11 先存纯文本、保留换行；富文本与 XSS 清洗留到 T12 公告统一做，见 D76） */
        @Size(max = 500, message = "个人简介不能超过 500 个字符")
        String bio
) {
}
