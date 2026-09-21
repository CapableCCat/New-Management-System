package com.tsguosc.dto;

import java.time.LocalDateTime;

/**
 * 报名提交结果。
 *
 * @param phone       提交使用的手机号（成功页展示，提示去哪儿查进度）
 * @param submittedAt 提交/重新提交时间
 * @param resubmitted 是否为"被拒后重新提交"（前端据此换文案）
 */
public record RecruitSubmitVO(String phone, LocalDateTime submittedAt, boolean resubmitted) {
}
