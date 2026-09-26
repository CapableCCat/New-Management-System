import request from './request'

/**
 * 基础看板（PRD F-010）
 *
 * ⚠️ 两个接口是**两套数据源**，PRD 特意强调不混用：
 *   - 成员现状：`user` 表，仅 status=正常（回答"现在有多少人"）
 *   - 招新复盘：`recruit_apply` 表，全量含被拒者（回答"这次纳新收成如何"）
 */

/** 成员现状：总人数 + 学院 / 专业 / 性别 / 省份分布 */
export const getMemberStats = () => request.get('/dashboard/member-stats')

/** 招新复盘：报名总数 / 待审 / 通过 / 拒绝 + 按日趋势 */
export const getRecruitStats = () => request.get('/dashboard/recruit-stats')
