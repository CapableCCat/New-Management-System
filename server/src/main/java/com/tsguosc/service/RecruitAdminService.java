package com.tsguosc.service;

import com.tsguosc.dto.PageResult;
import com.tsguosc.dto.RecruitApplyVO;
import com.tsguosc.dto.RecruitApproveBatchRequest;
import com.tsguosc.dto.RecruitApproveRequest;
import com.tsguosc.dto.RecruitPasswordVO;
import com.tsguosc.dto.RecruitQuery;
import com.tsguosc.dto.RecruitRejectRequest;
import com.tsguosc.dto.RecruitStatsVO;

/**
 * 审核管理台（F-003，纳新主链核心）。
 *
 * <p>数据隔离：社长团/超管可评审全部；部长只能评审"意向部门包含本部门"的记录
 * （列表过滤 + 单条操作二次校验，均在后端）。
 */
public interface RecruitAdminService {

    PageResult<RecruitApplyVO> list(RecruitQuery query);

    RecruitStatsVO stats();

    /** 单条通过：建号（手机号为主键、随机初始密码、首登强制改密）并留痕 */
    RecruitPasswordVO approve(RecruitApproveRequest request);

    /** 批量通过：逐条建号，失败项不影响其它项，返回密码清单与失败原因 */
    RecruitPasswordVO approveBatch(RecruitApproveBatchRequest request);

    /** 拒绝：原因必填，记录审核人与审核时间 */
    void reject(RecruitRejectRequest request);
}
