package com.tsguosc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.common.constant.DictType;
import com.tsguosc.dto.DashboardMemberStatsVO;
import com.tsguosc.dto.DashboardRecruitStatsVO;
import com.tsguosc.dto.NameValueVO;
import com.tsguosc.dto.TrendPointVO;
import com.tsguosc.entity.RecruitApply;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.RecruitApplyMapper;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.DashboardService;
import com.tsguosc.util.DictIndex;
import com.tsguosc.util.ProvinceNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基础看板实现（PRD F-010）。
 *
 * <p>三条口径上的讲究：
 * <ol>
 *   <li><b>两套数据源严格分开</b>：成员现状只数 `user` 且 `status=正常`；招新复盘数 `recruit_apply` 全量
 *       （含被拒者）。PRD 特意强调"两处数据源不混用"，因为它们的业务含义完全不同
 *       （一个回答"现在有多少人"，一个回答"这次纳新收成如何"）</li>
 *   <li><b>聚合放在 Java 侧</b>：社团队伍规模是几百到几千行，`selectList` + 内存分组足够，
 *       不为统计单独写 Mapper XML（与项目"能用 Wrapper 表达就不写 XML"的既有口径一致）。
 *       规模真上来了再把这些统计下沉到 SQL</li>
 *   <li><b>省份必须先归一化</b>：库里是「天津市 / Tianjin / 空」混着的手工输入，
 *       认不出来的**单列成「未填写」**、不上地图 —— 否则地图上会出现一块假区域（见 D100）</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int USER_STATUS_NORMAL = 0;
    private static final String UNSPECIFIED = "未填写";
    private static final String OTHER = "其他";
    private static final String CODE_OTHER = "other";

    /** 趋势补零的最大跨度：更长的（如隔年导入的历史数据）就只给有数据的日期，免得图上全是 0 */
    private static final int MAX_TREND_DAYS = 92;

    private final UserMapper userMapper;
    private final RecruitApplyMapper recruitApplyMapper;
    private final DictIndex dictIndex;

    // ------------------------------------------------------------
    // 成员现状
    // ------------------------------------------------------------

    @Override
    public DashboardMemberStatsVO memberStats() {
        Map<String, Map<String, String>> dict = dictIndex.codeToLabel();
        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery().eq(User::getStatus, USER_STATUS_NORMAL));

        Map<String, Long> colleges = new LinkedHashMap<>();
        Map<String, Long> majors = new LinkedHashMap<>();
        Map<String, Long> genders = new LinkedHashMap<>();
        Map<String, Long> provinces = new LinkedHashMap<>();
        long provinceUnspecified = 0;

        for (User user : users) {
            bump(colleges, dictLabel(dict, DictType.COLLEGE, user.getCollege()));
            bump(majors, majorName(user, dict));
            bump(genders, genderName(user.getGender()));

            String province = ProvinceNormalizer.normalize(user.getProvince());
            if (province == null) {
                provinceUnspecified++;
            } else {
                bump(provinces, province);
            }
        }

        log.info("看板·成员现状：正常成员 {} 人，省份未识别 {} 人", users.size(), provinceUnspecified);
        return new DashboardMemberStatsVO(
                users.size(), provinceUnspecified,
                sorted(colleges), sorted(majors), sorted(genders), sorted(provinces));
    }

    // ------------------------------------------------------------
    // 招新复盘
    // ------------------------------------------------------------

    @Override
    public DashboardRecruitStatsVO recruitStats() {
        List<RecruitApply> applies = recruitApplyMapper.selectList(Wrappers.<RecruitApply>lambdaQuery()
                .orderByAsc(RecruitApply::getCreatedAt)
                .orderByAsc(RecruitApply::getId));

        long pending = 0;
        long approved = 0;
        long rejected = 0;
        Map<LocalDate, Long> byDay = new LinkedHashMap<>();

        for (RecruitApply apply : applies) {
            if (Objects.equals(apply.getStatus(), RecruitApply.STATUS_APPROVED)) {
                approved++;
            } else if (Objects.equals(apply.getStatus(), RecruitApply.STATUS_REJECTED)) {
                rejected++;
            } else {
                pending++;
            }
            if (apply.getCreatedAt() != null) {
                LocalDate day = apply.getCreatedAt().toLocalDate();
                byDay.merge(day, 1L, Long::sum);
            }
        }

        List<TrendPointVO> trend = fillMissingDays(byDay);
        log.info("看板·招新复盘：报名 {} 条（待审 {} / 通过 {} / 拒绝 {}），趋势 {} 天",
                applies.size(), pending, approved, rejected, trend.size());
        return new DashboardRecruitStatsVO(applies.size(), pending, approved, rejected, trend);
    }

    /** 中间没有报名的日子补 0，折线才连续；跨度超过 {@link #MAX_TREND_DAYS} 时只给有数据的日期 */
    private List<TrendPointVO> fillMissingDays(Map<LocalDate, Long> byDay) {
        if (byDay.isEmpty()) {
            return List.of();
        }
        List<LocalDate> days = new ArrayList<>(byDay.keySet());
        days.sort(Comparator.naturalOrder());
        LocalDate start = days.get(0);
        LocalDate end = days.get(days.size() - 1);
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_TREND_DAYS) {
            return days.stream().map(day -> new TrendPointVO(day.toString(), byDay.get(day))).toList();
        }
        List<TrendPointVO> trend = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            trend.add(new TrendPointVO(day.toString(), byDay.getOrDefault(day, 0L)));
        }
        return trend;
    }

    // ------------------------------------------------------------
    // 小工具
    // ------------------------------------------------------------

    private void bump(Map<String, Long> counter, String key) {
        counter.merge(key, 1L, Long::sum);
    }

    /** 人数降序 → 名称升序（同人数时顺序稳定，图例不会乱跳） */
    private List<NameValueVO> sorted(Map<String, Long> counter) {
        return counter.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<String, Long>::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new NameValueVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** 字典 code → 中文标签；未填给「未填写」，查不到（如字典条目被删）归入「其他」并记日志 */
    private String dictLabel(Map<String, Map<String, String>> dict, DictType type, Object code) {
        if (code == null) {
            return UNSPECIFIED;
        }
        String key = String.valueOf(code).trim();
        Map<String, String> labels = dict.get(type.getCode());
        String label = labels == null ? null : labels.get(key);
        if (label == null) {
            log.warn("看板统计遇到未知的{}编码：{}（已归入「其他」）", type.getLabel(), key);
            return OTHER;
        }
        return label;
    }

    /** 专业：选「其他」的归到「其他」（不再细分手填文本，否则图上会碎成一堆 1 人项） */
    private String majorName(User user, Map<String, Map<String, String>> dict) {
        if (user.getMajor() == null) {
            return UNSPECIFIED;
        }
        if (CODE_OTHER.equalsIgnoreCase(user.getMajor())) {
            return OTHER;
        }
        return dictLabel(dict, DictType.MAJOR, user.getMajor());
    }

    /** 性别 0未填 / 1男 / 2女（§6 D6） */
    private String genderName(Integer gender) {
        if (gender == null) {
            return UNSPECIFIED;
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> UNSPECIFIED;
        };
    }
}
