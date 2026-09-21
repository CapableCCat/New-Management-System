package com.tsguosc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tsguosc.common.constant.DictType;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.PageResult;
import com.tsguosc.dto.RecruitApplyVO;
import com.tsguosc.dto.RecruitApproveBatchRequest;
import com.tsguosc.dto.RecruitApproveRequest;
import com.tsguosc.dto.RecruitPasswordVO;
import com.tsguosc.dto.RecruitQuery;
import com.tsguosc.dto.RecruitRejectRequest;
import com.tsguosc.dto.RecruitStatsVO;
import com.tsguosc.entity.RecruitApply;
import com.tsguosc.entity.SysDict;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.RecruitApplyMapper;
import com.tsguosc.mapper.SysDictMapper;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.RecruitAdminService;
import com.tsguosc.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 审核管理台实现。
 *
 * <p>关键规则：
 * <ul>
 *   <li>只有 `status=待审` 的记录可被通过/拒绝（重复审核防护）</li>
 *   <li>建号字段：手机号（主键）、BCrypt 随机初始密码、`status=0`、**`activated_at=null`**（首登强制改密）</li>
 *   <li>手机号已存在于 user 表 → 拒绝并提示「该手机号已存在账号，请检查」（PRD F-003）</li>
 *   <li>部长只能把新生分配到**本部门**（不能跨部门指派）</li>
 *   <li>明文初始密码只在响应里出现一次，不落库</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitAdminServiceImpl implements RecruitAdminService {

    private static final int ROLE_SUPER_ADMIN = 2;
    private static final int DEPARTMENT_LEADER_GROUP = 0;
    private static final int DUTY_MINISTER = 2;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String CODE_PATTERN = "\\d{1,3}";

    private final RecruitApplyMapper recruitApplyMapper;
    private final UserMapper userMapper;
    private final SysDictMapper sysDictMapper;
    private final PasswordEncoder passwordEncoder;

    /** 当前用户的评审范围 */
    private record ReviewScope(boolean all, String department) {
    }

    @Override
    public PageResult<RecruitApplyVO> list(RecruitQuery query) {
        ReviewScope scope = currentScope();
        Page<RecruitApply> page = new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize()));
        IPage<RecruitApply> result = recruitApplyMapper.selectPage(page, buildWrapper(query, scope));
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(),
                result.getRecords().stream().map(RecruitApplyVO::from).toList());
    }

    @Override
    public RecruitStatsVO stats() {
        ReviewScope scope = currentScope();
        return new RecruitStatsVO(
                countInScope(scope, RecruitApply.STATUS_PENDING),
                countInScope(scope, RecruitApply.STATUS_APPROVED),
                countInScope(scope, RecruitApply.STATUS_REJECTED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecruitPasswordVO approve(RecruitApproveRequest request) {
        RecruitApply apply = requireApply(request.id());
        ReviewScope scope = currentScope();
        assertCanReview(apply, scope);
        assertPending(apply);

        String department = resolveDepartment(request.department(), apply, scope);
        String password = createSingleAccount(apply, department, defaultDuty(request.duty()));
        markApproved(apply);
        return RecruitPasswordVO.single(apply.getName(), apply.getPhone(), password);
    }

    @Override
    public RecruitPasswordVO approveBatch(RecruitApproveBatchRequest request) {
        ReviewScope scope = currentScope();
        int duty = defaultDuty(request.duty());
        List<RecruitPasswordVO.Credential> credentials = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (Long id : new LinkedHashSet<>(request.ids())) {
            RecruitApply apply = null;
            try {
                apply = requireApply(id);
                assertCanReview(apply, scope);
                assertPending(apply);
                String department = resolveDepartment(null, apply, scope);
                String password = createSingleAccount(apply, department, duty);
                markApproved(apply);
                credentials.add(new RecruitPasswordVO.Credential(apply.getName(), apply.getPhone(), password));
            } catch (BusinessException e) {
                String who = apply == null ? ("id=" + id) : (apply.getName() + "(" + apply.getPhone() + ")");
                failures.add(who + "：" + e.getMessage());
            }
        }
        log.info("批量通过：成功 {} 条，失败 {} 条", credentials.size(), failures.size());
        return new RecruitPasswordVO(credentials.size(), failures.size(), credentials, failures);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(RecruitRejectRequest request) {
        RecruitApply apply = requireApply(request.id());
        assertCanReview(apply, currentScope());
        assertPending(apply);

        RecruitApply update = new RecruitApply();
        update.setId(apply.getId());
        update.setStatus(RecruitApply.STATUS_REJECTED);
        update.setRejectReason(request.reason().trim());
        update.setReviewerId(currentUserId());
        update.setReviewedAt(LocalDateTime.now());
        recruitApplyMapper.updateById(update);

        log.info("拒绝报名：id={}, phone={}", apply.getId(), apply.getPhone());
    }

    // ------------------------------------------------------------
    // 查询与范围
    // ------------------------------------------------------------

    private LambdaQueryWrapper<RecruitApply> buildWrapper(RecruitQuery query, ReviewScope scope) {
        LambdaQueryWrapper<RecruitApply> wrapper = Wrappers.<RecruitApply>lambdaQuery();
        if (query.getStatus() != null) {
            wrapper.eq(RecruitApply::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getCollege())) {
            wrapper.eq(RecruitApply::getCollege, query.getCollege().trim());
        }
        // 部长：范围锁定本部门；社长团/超管：可用筛选条件里的部门
        String department = scope.all() ? trimToNull(query.getDepartment()) : scope.department();
        if (department != null) {
            if (!department.matches(CODE_PATTERN)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "部门筛选值不合法");
            }
            wrapper.apply("JSON_CONTAINS(intent_departments, {0})", "[\"" + department + "\"]");
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(RecruitApply::getName, keyword).or().like(RecruitApply::getPhone, keyword));
        }
        // 先到先审
        wrapper.orderByAsc(RecruitApply::getCreatedAt).orderByAsc(RecruitApply::getId);
        return wrapper;
    }

    private long countInScope(ReviewScope scope, int status) {
        LambdaQueryWrapper<RecruitApply> wrapper = Wrappers.<RecruitApply>lambdaQuery()
                .eq(RecruitApply::getStatus, status);
        if (!scope.all()) {
            wrapper.apply("JSON_CONTAINS(intent_departments, {0})", "[\"" + scope.department() + "\"]");
        }
        Long count = recruitApplyMapper.selectCount(wrapper);
        return count == null ? 0 : count;
    }

    /** 社长团/超管：全部；部长：本部门；其余：无权限 */
    private ReviewScope currentScope() {
        User me = currentUser();
        if (Objects.equals(me.getRole(), ROLE_SUPER_ADMIN)
                || Objects.equals(me.getDepartment(), DEPARTMENT_LEADER_GROUP)) {
            return new ReviewScope(true, null);
        }
        if (Objects.equals(me.getDuty(), DUTY_MINISTER) && me.getDepartment() != null) {
            return new ReviewScope(false, String.valueOf(me.getDepartment()));
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "没有审核权限");
    }

    private void assertCanReview(RecruitApply apply, ReviewScope scope) {
        if (scope.all()) {
            return;
        }
        List<String> intents = apply.getIntentDepartments();
        if (intents == null || !intents.contains(scope.department())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该报名未选择你所在部门，无法评审");
        }
    }

    /** 部长只能分配到自己部门；社长团/超管可用传入值（为空时取意向部门第一个） */
    private String resolveDepartment(String requested, RecruitApply apply, ReviewScope scope) {
        String department;
        if (!scope.all()) {
            department = scope.department();
        } else if (StringUtils.hasText(requested)) {
            department = requested.trim();
        } else {
            List<String> intents = apply.getIntentDepartments();
            if (intents == null || intents.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "该报名没有意向部门，请手动指定部门");
            }
            department = intents.get(0);
        }
        requireEnabledDict(DictType.DEPARTMENT, department);
        return department;
    }

    // ------------------------------------------------------------
    // 建号
    // ------------------------------------------------------------

    private String createSingleAccount(RecruitApply apply, String department, int duty) {
        boolean exists = userMapper.exists(Wrappers.<User>lambdaQuery().eq(User::getPhone, apply.getPhone()));
        if (exists) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该手机号已存在账号，请检查");
        }
        String password = PasswordGenerator.random(8);

        User user = new User();
        user.setPhone(apply.getPhone());
        user.setName(apply.getName());
        user.setPassword(passwordEncoder.encode(password));
        user.setCollege(apply.getCollege());
        user.setMajor(apply.getMajor());
        user.setMajorText(apply.getMajorText());
        user.setDepartment(Integer.valueOf(department));
        user.setDuty(duty);
        user.setRole(0);
        user.setStatus(0);
        user.setGender(apply.getGender() == null ? 0 : apply.getGender());
        user.setProvince(apply.getProvince());
        user.setCity(apply.getCity());
        // 首登强制改密：随机初始密码必须换掉（PRD F-003 / §6 D35）
        user.setActivatedAt(null);
        userMapper.insert(user);

        log.info("审核通过并建号：applyId={}, userId={}, phone={}", apply.getId(), user.getId(), apply.getPhone());
        return password;
    }

    /** 写审核留痕：状态=通过 + 审核人/时间 + 回填 user_id（溯源用） */
    private void markApproved(RecruitApply apply) {
        RecruitApply update = new RecruitApply();
        update.setId(apply.getId());
        update.setStatus(RecruitApply.STATUS_APPROVED);
        update.setReviewerId(currentUserId());
        update.setReviewedAt(LocalDateTime.now());
        update.setUserId(findUserIdByPhone(apply.getPhone()));
        recruitApplyMapper.updateById(update);
    }

    private Long findUserIdByPhone(String phone) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone).last("LIMIT 1"));
        return user == null ? null : user.getId();
    }

    // ------------------------------------------------------------
    // 校验与小工具
    // ------------------------------------------------------------

    private RecruitApply requireApply(Long id) {
        RecruitApply apply = recruitApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }
        return apply;
    }

    private void assertPending(RecruitApply apply) {
        if (!Objects.equals(apply.getStatus(), RecruitApply.STATUS_PENDING)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该报名已审核过，无需重复操作");
        }
    }

    private void requireEnabledDict(DictType type, String code) {
        boolean exists = sysDictMapper.exists(Wrappers.<SysDict>lambdaQuery()
                .eq(SysDict::getType, type.getCode())
                .eq(SysDict::getCode, code)
                .eq(SysDict::getEnabled, 1));
        if (!exists) {
            throw new BusinessException(ResultCode.PARAM_ERROR, type.getLabel() + "选项不合法：" + code);
        }
    }

    private User currentUser() {
        User me = userMapper.selectById(currentUserId());
        if (me == null || Objects.equals(me.getStatus(), 1)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在或已冻结");
        }
        return me;
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private int defaultDuty(Integer duty) {
        return duty == null ? 0 : duty;
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
