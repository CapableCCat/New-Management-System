package com.tsguosc.service;

import com.tsguosc.dto.DashboardMemberStatsVO;
import com.tsguosc.dto.DashboardRecruitStatsVO;

/**
 * 基础看板（PRD F-010）。
 *
 * <p>权限：PRD 权限矩阵里「看板查看」对成员到超管**全员开放**，故本层与 Controller 都不加角色限制
 * （路径两段、不在白名单内 → 只要求登录）。
 */
public interface DashboardService {

    /** 成员现状：`user` 表**仅 status=正常**，按学院 / 专业 / 性别 / 省份分布 */
    DashboardMemberStatsVO memberStats();

    /** 招新复盘：`recruit_apply` **全量**，总数 / 通过 / 拒绝 / 待审 + 按日趋势 */
    DashboardRecruitStatsVO recruitStats();
}
