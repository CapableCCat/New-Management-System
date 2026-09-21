package com.tsguosc.service;

import com.tsguosc.dto.RecruitApplyRequest;
import com.tsguosc.dto.RecruitSubmitVO;

/**
 * 公开报名（F-001）：写 recruit_apply，**不建账号**。
 */
public interface RecruitService {

    /**
     * 提交报名。
     *
     * <p>手机号三分支：新号插入待审；待审/已通过则友好拒绝（不覆盖）；
     * 已拒绝则更新原记录并把状态重置为待审。
     */
    RecruitSubmitVO submit(RecruitApplyRequest request);
}
