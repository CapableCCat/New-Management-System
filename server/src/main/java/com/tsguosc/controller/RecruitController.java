package com.tsguosc.controller;

import com.tsguosc.common.result.Result;
import com.tsguosc.dto.RecruitApplyRequest;
import com.tsguosc.dto.RecruitInfoVO;
import com.tsguosc.dto.RecruitSubmitVO;
import com.tsguosc.service.RecruitService;
import com.tsguosc.service.SysConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开报名接口（免登录，见 SaTokenConfig 白名单的 /recruit/*）。
 *
 * <p>数据隔离：报名只写 recruit_apply，不建账号；账号在 T7 审核通过时才创建。
 */
@RestController
@RequestMapping("/recruit")
@RequiredArgsConstructor
public class RecruitController {

    private final RecruitService recruitService;
    private final SysConfigService sysConfigService;

    /** 报名页所需配置：报名开关 + 社团简介 + 审核时效文案 */
    @GetMapping("/info")
    public Result<RecruitInfoVO> info() {
        return Result.ok(sysConfigService.recruitInfo());
    }

    /** 提交报名（需图形验证码，验证码一次性作废） */
    @PostMapping("/apply")
    public Result<RecruitSubmitVO> apply(@Valid @RequestBody RecruitApplyRequest request) {
        RecruitSubmitVO result = recruitService.submit(request);
        return Result.ok(result, result.resubmitted() ? "已重新提交，请留意审核结果" : "报名提交成功");
    }
}
