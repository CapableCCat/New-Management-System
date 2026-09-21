package com.tsguosc.dto;

/**
 * 审核台统计（按当前用户的可见范围统计）。
 */
public record RecruitStatsVO(long pending, long approved, long rejected) {

    public long total() {
        return pending + approved + rejected;
    }
}
