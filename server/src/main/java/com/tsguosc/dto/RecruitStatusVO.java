package com.tsguosc.dto;

/**
 * 审核状态查询结果（F-005）。
 *
 * <p>安全要求（PRD F-005）：只返回状态与拒绝原因，不携带姓名 / 学院 / 专业 / 意向部门
 * 等任何其他信息。
 *
 * @param status       0 待审 / 1 已通过 / 2 已拒绝（recruit_apply 自己的枚举，见 D58）
 * @param rejectReason 拒绝原因（仅 status=2 时有值）
 */
public record RecruitStatusVO(Integer status, String rejectReason) {
}
