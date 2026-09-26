package com.tsguosc.controller;

import com.tsguosc.common.result.Result;
import com.tsguosc.dto.RecruitApplyRequest;
import com.tsguosc.dto.RecruitInfoVO;
import com.tsguosc.dto.RecruitStatusRequest;
import com.tsguosc.dto.RecruitStatusVO;
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

    /**
     * 提交报名（需图形验证码，验证码一次性作废）。
     *
     * <p>四种结果都走 {@code 200}：新提交 / 被拒后重提是成功；「该手机号已提交过报名」
     * 「该手机号已是正式成员」是**引导**（不是错误，见清单 §6 D111）——
     * 前端按 {@code data.nextAction} 就地渲染按钮，不再弹红色报错。
     */
    @PostMapping("/apply")
    public Result<RecruitSubmitVO> apply(@Valid @RequestBody RecruitApplyRequest request) {
        RecruitSubmitVO result = recruitService.submit(request);
        return Result.ok(result, result.message());
    }

    /**
     * 查询审核状态（F-005，公开；需图形验证码，一次性作废）。
     *
     * <p>只返回状态与拒绝原因；无记录时 {@code data} 为 {@code null}，用 message 给出友好文案
     * （走 200 而非错误码，前端可安静地渲染成提示卡片，不弹红色报错）。
     */
    @PostMapping("/status")
    public Result<RecruitStatusVO> status(@Valid @RequestBody RecruitStatusRequest request) {
        RecruitStatusVO result = recruitService.queryStatus(request);
        if (result == null) {
            return Result.ok(null, "未找到该手机号的报名记录，请确认手机号是否正确");
        }
        return Result.ok(result);
    }
}
