package com.tsguosc.service;

import com.tsguosc.dto.MemberQuery;

/**
 * 数据导出（PRD F-013）。
 *
 * <p>权限：PRD 明确「社长团 / 超管可导出，其他角色不可」——判定在 Controller 的角色注解上。
 * 由于导出者本身就是全量可见的干部，**导出列不做裁剪**（手机号 / 学号都能导），
 * 「导出列按权限控制」这条在 V1.0 由角色闸门本身满足；哪天放开给部长再按列裁剪。
 */
public interface ExportService {

    /**
     * 成员名册（`成员名册_YYYYMMDD.xlsx`）。
     *
     * @param query 复用成员档案页的筛选条件（关键字 / 学院 / 部门 / 职位 / 状态），
     *              即 PRD 的「支持按当前筛选条件导出」；不传则导全量
     */
    byte[] exportMemberRoster(MemberQuery query);

    /**
     * 报名/审核数据（`报名数据_YYYYMMDD.xlsx`）。
     *
     * <p>按 PRD 原文取 **`recruit_apply` 全量**（不分状态），含拒绝原因、审核人、审核时间等留痕字段。
     */
    byte[] exportRecruitApplies();
}
