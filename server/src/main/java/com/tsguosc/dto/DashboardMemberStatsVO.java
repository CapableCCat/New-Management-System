package com.tsguosc.dto;

import java.util.List;

/**
 * 看板「成员现状」（PRD F-010）。
 *
 * <p>⚠️ 口径：只统计 `user` 表里 **status = 正常** 的正式成员 —— 与「招新复盘」（`recruit_apply` 全量）
 * 是两套数据源，PRD 明确要求**不混用**。
 *
 * @param total               总人数
 * @param provinceUnspecified 生源地无法识别（含未填）的人数 —— 单列出来、不往地图上着色，
 *                            否则会出现一块假的区域
 * @param colleges            学院分布（字典 label）
 * @param majors              专业分布（字典 label；选「其他」的归入「其他」）
 * @param genders             性别分布（未填 / 男 / 女）
 * @param provinces           省份分布（**标准省名**，含台湾省 / 香港特别行政区 / 澳门特别行政区）
 */
public record DashboardMemberStatsVO(
        long total,
        long provinceUnspecified,
        List<NameValueVO> colleges,
        List<NameValueVO> majors,
        List<NameValueVO> genders,
        List<NameValueVO> provinces
) {
}
