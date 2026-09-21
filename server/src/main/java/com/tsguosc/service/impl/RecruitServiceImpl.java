package com.tsguosc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.common.constant.DictType;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.RecruitApplyRequest;
import com.tsguosc.dto.RecruitStatusRequest;
import com.tsguosc.dto.RecruitStatusVO;
import com.tsguosc.dto.RecruitSubmitVO;
import com.tsguosc.entity.RecruitApply;
import com.tsguosc.entity.SysDict;
import com.tsguosc.mapper.RecruitApplyMapper;
import com.tsguosc.mapper.SysDictMapper;
import com.tsguosc.service.RecruitService;
import com.tsguosc.service.SysConfigService;
import com.tsguosc.util.CaptchaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 公开报名实现。
 *
 * <p>顺序：验证码（一次性）→ 报名开关 → 字典编码合法性 → 专业「其他」手填校验 → 手机号三分支。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitServiceImpl implements RecruitService {

    /** 字典里「其他」的固定编码 */
    private static final String CODE_OTHER = "other";

    private final RecruitApplyMapper recruitApplyMapper;
    private final SysDictMapper sysDictMapper;
    private final SysConfigService sysConfigService;
    private final CaptchaValidator captchaValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecruitSubmitVO submit(RecruitApplyRequest request) {
        // 1. 图形验证码（无论对错都作废）
        captchaValidator.validateAndConsume(request.captchaKey(), request.captchaCode());

        // 2. 报名开关
        if (!sysConfigService.recruitInfo().open()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "本轮纳新报名已结束，感谢你对开源鸿蒙社的关注");
        }

        // 3. 字典编码合法性（防止前端伪造 code 落库）
        String college = request.college().trim();
        String major = request.major().trim();
        requireEnabledDict(DictType.COLLEGE, college);
        requireEnabledDict(DictType.MAJOR, major);
        List<String> departments = distinct(request.intentDepartments());
        departments.forEach(code -> requireEnabledDict(DictType.DEPARTMENT, code));
        List<String> tags = distinct(request.tags());
        tags.forEach(code -> requireEnabledDict(DictType.TAG, code));

        // 3.5 兴趣标签选「其他（自由补充）」时才保留手填值
        String tagText = trimToNull(request.tagText());
        if (!tags.contains(CODE_OTHER)) {
            tagText = null;
        }

        // 4. 专业选「其他」时必须手填
        String majorText = trimToNull(request.majorText());
        if (CODE_OTHER.equalsIgnoreCase(major) && majorText == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "选择「其他」专业时，请填写具体专业名称");
        }
        if (!CODE_OTHER.equalsIgnoreCase(major)) {
            majorText = null;
        }

        String name = request.name().trim();
        String phone = request.phone().trim();
        Integer gender = request.gender() == null ? 0 : request.gender();
        LocalDateTime now = LocalDateTime.now();

        RecruitApply existing = recruitApplyMapper.selectOne(
                Wrappers.<RecruitApply>lambdaQuery().eq(RecruitApply::getPhone, phone));

        if (existing == null) {
            RecruitApply entity = new RecruitApply();
            entity.setName(name);
            entity.setPhone(phone);
            entity.setCollege(college);
            entity.setMajor(major);
            entity.setMajorText(majorText);
            entity.setIntentDepartments(departments);
            entity.setTags(tags.isEmpty() ? null : tags);
            entity.setTagText(tagText);
            entity.setGender(gender);
            entity.setProvince(trimToNull(request.province()));
            entity.setCity(trimToNull(request.city()));
            entity.setStatus(RecruitApply.STATUS_PENDING);
            recruitApplyMapper.insert(entity);
            log.info("收到新报名：phone={}, college={}, major={}", phone, college, major);
            return new RecruitSubmitVO(phone, entity.getCreatedAt() == null ? now : entity.getCreatedAt(), false);
        }

        if (Objects.equals(existing.getStatus(), RecruitApply.STATUS_PENDING)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "该手机号已提交过报名，可在「查询审核状态」页查看进度");
        }
        if (Objects.equals(existing.getStatus(), RecruitApply.STATUS_APPROVED)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "该手机号已通过审核，请直接登录系统；如需修改信息请到个人中心");
        }

        // 5. 被拒后重新提交：覆盖表单内容 + 状态重置为待审 + 清空上一次审核留痕
        RecruitApply update = new RecruitApply();
        update.setId(existing.getId());
        update.setName(name);
        update.setCollege(college);
        update.setMajor(major);
        update.setIntentDepartments(departments);
        update.setTags(tags.isEmpty() ? null : tags);
        update.setGender(gender);
        update.setStatus(RecruitApply.STATUS_PENDING);
        // 视为一次新提交：刷新提交时间，保证审核台按时间排序时不会被排到最下面
        update.setCreatedAt(now);
        recruitApplyMapper.updateById(update);

        // ⚠️ 可空字段必须走显式 set：MyBatis-Plus 的 updateById 会忽略 null 字段，
        // 否则上次填过的「专业手填值 / 标签补充 / 生源地」会残留（脏数据）。
        recruitApplyMapper.update(null, Wrappers.<RecruitApply>lambdaUpdate()
                .eq(RecruitApply::getId, existing.getId())
                .set(RecruitApply::getMajorText, majorText)
                .set(RecruitApply::getTagText, tagText)
                .set(RecruitApply::getProvince, trimToNull(request.province()))
                .set(RecruitApply::getCity, trimToNull(request.city()))
                .set(RecruitApply::getRejectReason, null)
                .set(RecruitApply::getReviewerId, null)
                .set(RecruitApply::getReviewedAt, null));

        log.info("被拒后重新提交：phone={}, applyId={}", phone, existing.getId());
        return new RecruitSubmitVO(phone, now, true);
    }

    @Override
    public RecruitStatusVO queryStatus(RecruitStatusRequest request) {
        // 1. 图形验证码（一次性，无论对错都作废）—— 防脚本批量探测手机号
        captchaValidator.validateAndConsume(request.captchaKey(), request.captchaCode());

        // 2. 查报名记录：逻辑删除的记录由 @TableLogic 自动过滤，等同于「未找到」
        RecruitApply apply = recruitApplyMapper.selectOne(
                Wrappers.<RecruitApply>lambdaQuery().eq(RecruitApply::getPhone, request.phone().trim()));
        if (apply == null) {
            return null;
        }

        // 3. 只返回状态与拒绝原因，不泄露其它字段（PRD F-005 安全要求）
        return new RecruitStatusVO(apply.getStatus(), apply.getRejectReason());
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    /** 字典编码必须存在且处于启用状态 */
    private void requireEnabledDict(DictType type, String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择" + type.getLabel());
        }
        boolean exists = sysDictMapper.exists(Wrappers.<SysDict>lambdaQuery()
                .eq(SysDict::getType, type.getCode())
                .eq(SysDict::getCode, code.trim())
                .eq(SysDict::getEnabled, 1));
        if (!exists) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    type.getLabel() + "选项不合法，请刷新页面后重试");
        }
    }

    /** 去重并保持顺序 */
    private List<String> distinct(List<String> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String item : source) {
            if (StringUtils.hasText(item)) {
                set.add(item.trim());
            }
        }
        return new ArrayList<>(set);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
