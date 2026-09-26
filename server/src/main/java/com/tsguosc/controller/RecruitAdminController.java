package com.tsguosc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.tsguosc.common.constant.Roles;
import com.tsguosc.common.result.Result;
import com.tsguosc.dto.PageResult;
import com.tsguosc.dto.RecruitApplyVO;
import com.tsguosc.dto.RecruitApproveBatchRequest;
import com.tsguosc.dto.RecruitApproveRequest;
import com.tsguosc.dto.RecruitPasswordVO;
import com.tsguosc.dto.RecruitQuery;
import com.tsguosc.dto.RecruitRejectRequest;
import com.tsguosc.dto.RecruitSmsConfigVO;
import com.tsguosc.dto.RecruitStatsVO;
import com.tsguosc.service.ExportService;
import com.tsguosc.service.RecruitAdminService;
import com.tsguosc.util.ExcelExporter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 审核管理台接口（F-003）。
 *
 * <p>路径三段（/recruit/admin/**），不在拦截器白名单内 → 需登录；
 * 再由 @SaCheckRole 限定「超管 / 社长团 / 部长」三者之一，具体数据范围（部长只看本部门）
 * 在 service 里按当前用户推导。
 *
 * <p>例外：{@code /recruit/admin/export}（T14 数据导出）在方法上又加了一道更严的角色注解 ——
 * 类级与方法级的 {@code @SaCheckRole} 是**叠加**关系，所以最终效果是「只放超管 / 社长团」。
 */
@RestController
@RequestMapping("/recruit/admin")
@RequiredArgsConstructor
@SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP, Roles.MINISTER}, mode = SaMode.OR)
public class RecruitAdminController {

    /** 导出文件名里的日期（PRD F-013：报名数据_YYYYMMDD.xlsx） */
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RecruitAdminService recruitAdminService;
    private final ExportService exportService;

    /** 报名列表（分页 + 筛选；部长自动只看到意向含本部门的记录） */
    @GetMapping("/list")
    public Result<PageResult<RecruitApplyVO>> list(RecruitQuery query) {
        return Result.ok(recruitAdminService.list(query));
    }

    /** 审核台统计（按可见范围） */
    @GetMapping("/stats")
    public Result<RecruitStatsVO> stats() {
        return Result.ok(recruitAdminService.stats());
    }

    /** 单条通过：建号并返回明文初始密码（只出现这一次，不落库） */
    @PostMapping("/approve")
    public Result<RecruitPasswordVO> approve(@Valid @RequestBody RecruitApproveRequest request) {
        RecruitPasswordVO result = recruitAdminService.approve(request);
        return Result.ok(result, "已通过并创建账号");
    }

    /** 批量通过：逐条建号，返回密码清单与失败原因 */
    @PostMapping("/approve-batch")
    public Result<RecruitPasswordVO> approveBatch(@Valid @RequestBody RecruitApproveBatchRequest request) {
        RecruitPasswordVO result = recruitAdminService.approveBatch(request);
        String message = result.failedCount() == 0
                ? "批量通过完成，共 " + result.successCount() + " 条"
                : "批量通过完成：成功 " + result.successCount() + " 条，失败 " + result.failedCount() + " 条";
        return Result.ok(result, message);
    }

    /** 拒绝：原因必填（将展示给被拒者） */
    @PostMapping("/reject")
    public Result<Void> reject(@Valid @RequestBody RecruitRejectRequest request) {
        recruitAdminService.reject(request);
        return Result.ok(null, "已拒绝该报名");
    }

    /**
     * 短信通知提效工具所需配置（F-004）：系统链接 + 两个短信模板。
     *
     * <p>只读；供审核台干部（超管 / 社长团 / 部长）拼装通知话术用。
     */
    @GetMapping("/sms-config")
    public Result<RecruitSmsConfigVO> smsConfig() {
        return Result.ok(recruitAdminService.smsConfig());
    }

    /**
     * 导出报名/审核数据（PRD F-013：`报名数据_YYYYMMDD.xlsx`）。
     *
     * <p>按 PRD 取 `recruit_apply` **全量**（不分状态），含状态、拒绝原因、审核人、审核时间等留痕字段。
     * 方法级注解把权限收紧到超管 / 社长团（部长不能导出，PRD 明确「其他角色不可」）。
     */
    @GetMapping("/export")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP}, mode = SaMode.OR)
    public void exportApplies(HttpServletResponse response) throws IOException {
        byte[] data = exportService.exportRecruitApplies();
        String date = LocalDate.now().format(FILE_DATE);
        ExcelExporter.writeToResponse(response,
                "报名数据_" + date + ".xlsx", "recruit-applies_" + date + ".xlsx", data);
    }
}
