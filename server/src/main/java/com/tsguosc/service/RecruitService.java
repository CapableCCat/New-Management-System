package com.tsguosc.service;

import com.tsguosc.dto.RecruitApplyRequest;
import com.tsguosc.dto.RecruitStatusRequest;
import com.tsguosc.dto.RecruitStatusVO;
import com.tsguosc.dto.RecruitSubmitVO;

/**
 * 公开报名（F-001）：写 recruit_apply，**不建账号**；并提供公开的审核状态查询（F-005）。
 */
public interface RecruitService {

    /**
     * 提交报名。
     *
     * <p>手机号三分支：新号插入待审；待审/已通过则友好拒绝（不覆盖）；
     * 已拒绝则更新原记录并把状态重置为待审。
     */
    RecruitSubmitVO submit(RecruitApplyRequest request);

    /**
     * 查询审核状态（F-005，公开）。
     *
     * <p>凭手机号 + 一次性图形验证码查询；只返回状态与拒绝原因。
     * 无记录（含已逻辑删除）返回 {@code null}，由前端按「未找到」友好提示。
     */
    RecruitStatusVO queryStatus(RecruitStatusRequest request);
}
