package com.tsguosc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.common.constant.DictType;
import com.tsguosc.dto.MemberExportRow;
import com.tsguosc.dto.MemberQuery;
import com.tsguosc.dto.RecruitExportRow;
import com.tsguosc.entity.RecruitApply;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.RecruitApplyMapper;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.ExportService;
import com.tsguosc.util.DictIndex;
import com.tsguosc.util.ExcelExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据导出实现（PRD F-013）。
 *
 * <p>三件必须做对的小事：
 * <ol>
 *   <li><b>字典 code → 中文标签</b>：库里存的是 code，导出给人看的是名称。
 *       这里刻意把**停用项也一起 load**（不像 T13 导入只认启用项）—— 历史数据引用了后来被停用的
 *       学院/专业时，导出仍要显示它的名字，而不是甩一个 code 出来</li>
 *   <li><b>JSON 字段转可读文本</b>：意向部门 / 兴趣标签是 code 数组，导出成「技术部、宣传部」</li>
 *   <li><b>两套状态口径别混</b>：`recruit_apply.status`（待审/通过/拒绝）是它自己的枚举（§6 D58），
 *       与 `user.status`（字典 status）不是一回事，各走各的转换</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CODE_OTHER = "other";
    private static final String SHEET_MEMBERS = "成员名册";
    private static final String SHEET_APPLIES = "报名数据";
    private static final String UNASSIGNED = "未分配";

    private final UserMapper userMapper;
    private final RecruitApplyMapper recruitApplyMapper;
    private final DictIndex dictIndex;

    // ------------------------------------------------------------
    // 成员名册
    // ------------------------------------------------------------

    @Override
    public byte[] exportMemberRoster(MemberQuery query) {
        Map<String, Map<String, String>> dict = dictIndex.codeToLabel();
        List<User> users = userMapper.selectList(memberWrapper(query));
        List<MemberExportRow> rows = users.stream().map(user -> toMemberRow(user, dict)).toList();
        log.info("导出成员名册：{} 行（keyword={}, college={}, department={}, duty={}, status={}）",
                rows.size(), text(query.getKeyword()), text(query.getCollege()),
                query.getDepartment(), query.getDuty(), query.getStatus());
        return ExcelExporter.toXlsx(SHEET_MEMBERS, MemberExportRow.class, rows);
    }

    /** 与成员档案列表同一套筛选口径与排序（部门升序 → 职位降序 → id 升序） */
    private LambdaQueryWrapper<User> memberWrapper(MemberQuery query) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(User::getName, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getStudentId, keyword));
        }
        if (StringUtils.hasText(query.getCollege())) {
            wrapper.eq(User::getCollege, query.getCollege().trim());
        }
        if (query.getDepartment() != null) {
            wrapper.eq(User::getDepartment, query.getDepartment());
        }
        if (query.getDuty() != null) {
            wrapper.eq(User::getDuty, query.getDuty());
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        return wrapper.orderByAsc(User::getDepartment).orderByDesc(User::getDuty).orderByAsc(User::getId);
    }

    private MemberExportRow toMemberRow(User user, Map<String, Map<String, String>> dict) {
        MemberExportRow row = new MemberExportRow();
        row.setName(text(user.getName()));
        row.setPhone(text(user.getPhone()));
        row.setStudentId(text(user.getStudentId()));
        row.setCollege(label(dict, DictType.COLLEGE, user.getCollege(), ""));
        row.setMajor(majorText(user.getMajor(), user.getMajorText(), dict));
        row.setDepartment(label(dict, DictType.DEPARTMENT, user.getDepartment(), UNASSIGNED));
        row.setDuty(label(dict, DictType.DUTY, user.getDuty(), ""));
        row.setGender(genderText(user.getGender()));
        row.setStatus(label(dict, DictType.STATUS, user.getStatus(), ""));
        row.setProvince(text(user.getProvince()));
        row.setCity(text(user.getCity()));
        row.setCreatedAt(timeText(user.getCreatedAt()));
        return row;
    }

    // ------------------------------------------------------------
    // 报名 / 审核数据
    // ------------------------------------------------------------

    @Override
    public byte[] exportRecruitApplies() {
        Map<String, Map<String, String>> dict = dictIndex.codeToLabel();
        // PRD：recruit_apply 全量（不分状态）；按提交时间升序便于复盘
        List<RecruitApply> applies = recruitApplyMapper.selectList(Wrappers.<RecruitApply>lambdaQuery()
                .orderByAsc(RecruitApply::getCreatedAt)
                .orderByAsc(RecruitApply::getId));
        Map<Long, String> reviewers = reviewerNames(applies);
        List<RecruitExportRow> rows = applies.stream()
                .map(apply -> toApplyRow(apply, dict, reviewers))
                .toList();
        log.info("导出报名数据：{} 行", rows.size());
        return ExcelExporter.toXlsx(SHEET_APPLIES, RecruitExportRow.class, rows);
    }

    private RecruitExportRow toApplyRow(RecruitApply apply, Map<String, Map<String, String>> dict,
                                        Map<Long, String> reviewers) {
        RecruitExportRow row = new RecruitExportRow();
        row.setName(text(apply.getName()));
        row.setPhone(text(apply.getPhone()));
        row.setCollege(label(dict, DictType.COLLEGE, apply.getCollege(), ""));
        row.setMajor(majorText(apply.getMajor(), apply.getMajorText(), dict));
        row.setIntentDepartments(joinLabels(dict, DictType.DEPARTMENT, apply.getIntentDepartments()));
        row.setTags(joinTags(dict, apply));
        row.setGender(genderText(apply.getGender()));
        row.setProvince(text(apply.getProvince()));
        row.setCity(text(apply.getCity()));
        row.setStatus(applyStatusText(apply.getStatus()));
        row.setRejectReason(text(apply.getRejectReason()));
        row.setReviewerName(reviewers.getOrDefault(apply.getReviewerId(), ""));
        row.setReviewedAt(timeText(apply.getReviewedAt()));
        row.setCreatedAt(timeText(apply.getCreatedAt()));
        row.setUserId(apply.getUserId());
        return row;
    }

    /** 审核人姓名：一次批量查库，别逐行查（与 T12 公告作者名同一做法） */
    private Map<Long, String> reviewerNames(List<RecruitApply> applies) {
        Set<Long> ids = applies.stream()
                .map(RecruitApply::getReviewerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (first, second) -> first));
    }

    // ------------------------------------------------------------
    // 文本转换
    // ------------------------------------------------------------

    /** code → 中文标签；查不到就退回原始 code（比留空更有信息量），null 用 fallback */
    private String label(Map<String, Map<String, String>> dict, DictType type, Object code, String fallback) {
        if (code == null) {
            return fallback;
        }
        String key = String.valueOf(code).trim();
        Map<String, String> labels = dict.get(type.getCode());
        String value = labels == null ? null : labels.get(key);
        return value == null ? key : value;
    }

    /** 专业：选「其他」时给出原文，否则给字典名称 */
    private String majorText(String major, String majorTextValue, Map<String, Map<String, String>> dict) {
        if (major == null) {
            return "";
        }
        if (CODE_OTHER.equalsIgnoreCase(major)) {
            return StringUtils.hasText(majorTextValue) ? "其他（" + majorTextValue.trim() + "）" : "其他";
        }
        return label(dict, DictType.MAJOR, major, "");
    }

    private String joinLabels(Map<String, Map<String, String>> dict, DictType type, List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return "";
        }
        return codes.stream()
                .map(code -> label(dict, type, code, ""))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
    }

    /** 兴趣标签：code 数组 + 选「其他」时的自由补充文本 */
    private String joinTags(Map<String, Map<String, String>> dict, RecruitApply apply) {
        String labels = joinLabels(dict, DictType.TAG, apply.getTags());
        if (!StringUtils.hasText(apply.getTagText())) {
            return labels;
        }
        return labels.isEmpty() ? apply.getTagText().trim() : labels + "、" + apply.getTagText().trim();
    }

    /** 性别 0未填 / 1男 / 2女（§6 D6） */
    private String genderText(Integer gender) {
        if (gender == null) {
            return "未填";
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未填";
        };
    }

    /** 报名状态 0待审 / 1通过 / 2拒绝 —— 这是 recruit_apply 自己的枚举（§6 D58），不走字典 */
    private String applyStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case RecruitApply.STATUS_APPROVED -> "通过";
            case RecruitApply.STATUS_REJECTED -> "拒绝";
            default -> "待审";
        };
    }

    private String timeText(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
