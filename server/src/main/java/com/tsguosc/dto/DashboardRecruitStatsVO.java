package com.tsguosc.dto;

import java.util.List;

/**
 * 看板「招新复盘」（PRD F-010）。
 *
 * <p>⚠️ 口径：统计 `recruit_apply` **全量**（含被拒者），与「成员现状」是两套数据源，不混用。
 *
 * @param total    报名总数（全量）
 * @param pending  待审数（PRD 只列了总数/通过/拒绝，这里补上，与审核台口径一致）
 * @param approved 通过数
 * @param rejected 拒绝数
 * @param trend    按报名提交日归档的趋势（无报名的日子补 0）
 */
public record DashboardRecruitStatsVO(
        long total,
        long pending,
        long approved,
        long rejected,
        List<TrendPointVO> trend
) {
}
