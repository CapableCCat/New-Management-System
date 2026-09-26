package com.tsguosc.controller;

import com.tsguosc.common.result.Result;
import com.tsguosc.dto.DashboardMemberStatsVO;
import com.tsguosc.dto.DashboardRecruitStatsVO;
import com.tsguosc.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基础看板（PRD F-010）。
 *
 * <p>权限：PRD 权限矩阵里「看板查看」对成员 / 部长 / 社长团 / 超管**全员开放**，
 * 所以这里**不加 `@SaCheckRole`**；两段路径不在白名单内，天然要求登录。
 * 看板只返回聚合数字（不含任何个人信息），对普通成员开放无泄露风险。
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 成员现状：仅 status=正常的正式成员（学院 / 专业 / 性别 / 省份分布） */
    @GetMapping("/member-stats")
    public Result<DashboardMemberStatsVO> memberStats() {
        return Result.ok(dashboardService.memberStats());
    }

    /** 招新复盘：recruit_apply 全量（总数 / 待审 / 通过 / 拒绝 + 按日趋势） */
    @GetMapping("/recruit-stats")
    public Result<DashboardRecruitStatsVO> recruitStats() {
        return Result.ok(dashboardService.recruitStats());
    }
}
