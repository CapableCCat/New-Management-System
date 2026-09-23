package com.tsguosc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 成员档案编辑请求（F-006）。
 *
 * <p>这是**管理员侧**的整体编辑：请求里永远带齐可编辑字段（前端提交整表单），
 * 所以服务端必须用显式 set 落库 —— 否则「清空学号 / 生源地」这类操作会被
 * MyBatis-Plus 的 updateById 忽略 null 行为悄悄吞掉。
 *
 * <p>字段权限按当前角色在 service 里裁剪：
 * <ul>
 *   <li>社长团 / 超管：可改部门、职位</li>
 *   <li>部长：可改其余字段，但部门锁定本部门、职位不可改，且不能编辑自己</li>
 * </ul>
 * `role`（是否超管）**不开放编辑**，避免权限自提升。
 */
public record MemberUpdateRequest(

        @NotNull(message = "缺少成员 id")
        Long id,

        @NotBlank(message = "请填写姓名")
        @Size(max = 32, message = "姓名不能超过 32 个字符")
        String name,

        @NotBlank(message = "请填写手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        /** 学号：可空；传空串视为清空 */
        @Size(max = 32, message = "学号不能超过 32 个字符")
        String studentId,

        /** 学院 code：可空（未填则不校验字典） */
        String college,

        /** 专业 code：可空 */
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

        /** 部门 code 0~4（部长传了也会被后端忽略、保持本部门） */
        @Min(value = 0, message = "部门取值不合法")
        @Max(value = 4, message = "部门取值不合法")
        Integer department,

        @Min(value = 0, message = "职位取值不合法")
        @Max(value = 3, message = "职位取值不合法")
        Integer duty,

        /** 状态 0正常 1冻结 */
        @Min(value = 0, message = "状态取值不合法")
        @Max(value = 1, message = "状态取值不合法")
        Integer status
) {
}
