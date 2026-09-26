package com.tsguosc.dto;

import java.time.LocalDateTime;

/**
 * 报名提交结果（F-001）。
 *
 * <p>「提交报名」共有四种结果，其中后两种**不是错误、只是要引导**（见《开发任务点清单》§6 D111）——
 * 一律返回 {@code 200}，靠 {@link #state()} + {@link #nextAction()} 让前端就地渲染结果卡与按钮；
 * 错误码只留给真正的失败（验证码错、参数非法、报名已关闭）。
 *
 * @param phone       提交使用的手机号（结果卡展示）
 * @param submittedAt 提交 / 重新提交时间
 * @param state       结果状态：SUBMITTED / RESUBMITTED / ALREADY_PENDING / ALREADY_MEMBER
 * @param nextAction  下一步引导：QUERY（去查询审核状态）/ LOGIN（去登录）
 * @param message     直接展示给用户的一句话（axios 拦截器只脱壳 data，故文案随 data 下发）
 */
public record RecruitSubmitVO(
        String phone,
        LocalDateTime submittedAt,
        String state,
        String nextAction,
        String message) {

    /** 新报名提交成功 */
    public static final String STATE_SUBMITTED = "SUBMITTED";
    /** 被拒后重新提交 */
    public static final String STATE_RESUBMITTED = "RESUBMITTED";
    /** 该手机号已提交过报名且仍在待审 */
    public static final String STATE_ALREADY_PENDING = "ALREADY_PENDING";
    /** 该手机号已是正式成员 */
    public static final String STATE_ALREADY_MEMBER = "ALREADY_MEMBER";

    /** 引导去「查询审核状态」页 */
    public static final String NEXT_QUERY = "QUERY";
    /** 引导去登录页（已是正式成员） */
    public static final String NEXT_LOGIN = "LOGIN";
}
