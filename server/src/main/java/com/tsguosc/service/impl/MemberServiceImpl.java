package com.tsguosc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tsguosc.common.constant.DictType;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.MemberQuery;
import com.tsguosc.dto.MemberUpdateRequest;
import com.tsguosc.dto.PageResult;
import com.tsguosc.dto.UserVO;
import com.tsguosc.entity.SysDict;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.SysDictMapper;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 成员档案管理实现（F-006）。
 *
 * <p>两条范围规则都在这里落地：
 * <ul>
 *   <li><b>行范围</b>：部长只能看/改本部门成员；社长团、超管看/改全部；
 *       普通成员查看时不受行限制（成员展板本来就是全社展示）</li>
 *   <li><b>列范围</b>：普通成员拿到的手机号 / 学号被抹掉（PRD 第五章敏感列可见性）</li>
 * </ul>
 *
 * <p>防提权：部长不能改部门 / 职位，也不能编辑自己；`role` 不开放编辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private static final int ROLE_SUPER_ADMIN = 2;
    private static final int DEPARTMENT_LEADER_GROUP = 0;
    private static final int DUTY_MINISTER = 2;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String CODE_OTHER = "other";

    private final UserMapper userMapper;
    private final SysDictMapper sysDictMapper;

    /** 行范围：{@code all=true} 表示不限；否则限定 {@code department} */
    private record RowScope(boolean all, Integer department) {
    }

    // ------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------

    @Override
    public PageResult<UserVO> list(MemberQuery query) {
        User me = currentUser();
        RowScope scope = viewScope(me);
        boolean maskSensitive = !canManageDepartment(me);

        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        // 行范围：非全量范围时强制限定本部门（忽略前端传来的部门筛选）
        if (!scope.all()) {
            wrapper.eq(User::getDepartment, scope.department());
        } else if (query.getDepartment() != null) {
            wrapper.eq(User::getDepartment, query.getDepartment());
        }
        if (StringUtils.hasText(query.getCollege())) {
            wrapper.eq(User::getCollege, query.getCollege().trim());
        }
        if (query.getDuty() != null) {
            wrapper.eq(User::getDuty, query.getDuty());
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }

        String keyword = trimToNull(query.getKeyword());
        if (keyword != null) {
            if (maskSensitive) {
                // 手机号 / 学号对普通成员不可见，那也不能拿来当探测条件
                wrapper.like(User::getName, keyword);
            } else {
                wrapper.and(w -> w.like(User::getName, keyword)
                        .or().like(User::getPhone, keyword)
                        .or().like(User::getStudentId, keyword));
            }
        }
        wrapper.orderByAsc(User::getDepartment).orderByDesc(User::getDuty).orderByAsc(User::getId);

        Page<User> page = new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize()));
        IPage<User> result = userMapper.selectPage(page, wrapper);
        List<UserVO> records = result.getRecords().stream()
                .map(UserVO::from)
                .map(vo -> maskSensitive ? vo.masked() : vo)
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public UserVO detail(Long id) {
        User me = currentUser();
        User target = requireUser(id);
        assertInScope(target, viewScope(me), "无权查看该成员");

        UserVO vo = UserVO.from(target);
        return canManageDepartment(me) ? vo : vo.masked();
    }

    // ------------------------------------------------------------
    // 编辑
    // ------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MemberUpdateRequest request) {
        User me = currentUser();
        RowScope scope = editScope(me);
        User target = requireUser(request.id());
        assertInScope(target, scope, "无权编辑该成员");

        boolean fullEdit = canManageAll(me);
        if (!fullEdit && Objects.equals(target.getId(), me.getId())) {
            // 部长本身就在「本部门」范围内，若允许编辑自己就能把 duty 改成社长 → 权限自提升
            throw new BusinessException(ResultCode.FORBIDDEN, "部长不能编辑自己的档案，请联系社长团");
        }

        // 部长：部门锁死本部门、职位不可改
        Integer department = fullEdit ? request.department() : target.getDepartment();
        Integer duty = fullEdit ? (request.duty() == null ? target.getDuty() : request.duty()) : target.getDuty();
        Integer status = request.status() == null ? target.getStatus() : request.status();

        String name = request.name().trim();
        String phone = request.phone().trim();
        String studentId = trimToNull(request.studentId());
        String college = trimToNull(request.college());
        String major = trimToNull(request.major());
        String majorText = trimToNull(request.majorText());
        if (major == null || !CODE_OTHER.equalsIgnoreCase(major)) {
            majorText = null;
        } else if (majorText == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "选择「其他」专业时，请填写具体专业名称");
        }

        // 字典编码合法性（防前端伪造 code 落库，见 §6 D48）
        if (college != null) {
            requireEnabledDict(DictType.COLLEGE, college);
        }
        if (major != null) {
            requireEnabledDict(DictType.MAJOR, major);
        }
        if (department != null) {
            requireEnabledDict(DictType.DEPARTMENT, String.valueOf(department));
        }

        // 唯一性：手机号（登录主键）与学号；学号空串已归一为 NULL，MySQL 唯一索引允许多个 NULL
        if (!Objects.equals(phone, target.getPhone()) && phoneTaken(phone, target.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该手机号已被使用");
        }
        if (studentId != null && !Objects.equals(studentId, target.getStudentId())
                && studentIdTaken(studentId, target.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该学号已被使用");
        }

        // ⚠️ 显式 set：整表单提交意味着「清空学号 / 生源地」也是合法操作，
        // 走 updateById 会被 MyBatis-Plus 忽略 null 而静默失败（§6 D40 的同类坑）
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, target.getId())
                .set(User::getName, name)
                .set(User::getPhone, phone)
                .set(User::getStudentId, studentId)
                .set(User::getCollege, college)
                .set(User::getMajor, major)
                .set(User::getMajorText, majorText)
                .set(User::getGender, request.gender() == null ? target.getGender() : request.gender())
                .set(User::getProvince, trimToNull(request.province()))
                .set(User::getCity, trimToNull(request.city()))
                .set(User::getDepartment, department)
                .set(User::getDuty, duty)
                .set(User::getStatus, status)
                .set(User::getUpdatedBy, me.getId()));

        log.info("成员档案已更新：id={}, operator={}, phoneChanged={}, status={}",
                target.getId(), me.getId(), !Objects.equals(phone, target.getPhone()), status);
    }

    // ------------------------------------------------------------
    // 范围推导
    // ------------------------------------------------------------

    /** 查看范围：社长团/超管=全部；部长=本部门；普通成员=全部行（但只有基础列） */
    private RowScope viewScope(User me) {
        if (canManageAll(me)) {
            return new RowScope(true, null);
        }
        if (isMinister(me)) {
            return new RowScope(false, me.getDepartment());
        }
        return new RowScope(true, null);
    }

    /** 编辑范围：仅管理侧可用（普通成员调用会 40300） */
    private RowScope editScope(User me) {
        if (canManageAll(me)) {
            return new RowScope(true, null);
        }
        if (isMinister(me)) {
            return new RowScope(false, me.getDepartment());
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无成员档案编辑权限");
    }

    private void assertInScope(User target, RowScope scope, String message) {
        if (scope.all()) {
            return;
        }
        if (!Objects.equals(target.getDepartment(), scope.department())) {
            throw new BusinessException(ResultCode.FORBIDDEN, message);
        }
    }

    private boolean canManageAll(User user) {
        return Objects.equals(user.getRole(), ROLE_SUPER_ADMIN)
                || Objects.equals(user.getDepartment(), DEPARTMENT_LEADER_GROUP);
    }

    private boolean isMinister(User user) {
        return Objects.equals(user.getDuty(), DUTY_MINISTER);
    }

    /** 部门管理资格：部长或社长团/超管 */
    private boolean canManageDepartment(User user) {
        return isMinister(user) || canManageAll(user);
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    private boolean phoneTaken(String phone, Long excludeId) {
        return userMapper.exists(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, phone)
                .ne(User::getId, excludeId));
    }

    private boolean studentIdTaken(String studentId, Long excludeId) {
        return userMapper.exists(Wrappers.<User>lambdaQuery()
                .eq(User::getStudentId, studentId)
                .ne(User::getId, excludeId));
    }

    private void requireEnabledDict(DictType type, String code) {
        boolean exists = sysDictMapper.exists(Wrappers.<SysDict>lambdaQuery()
                .eq(SysDict::getType, type.getCode())
                .eq(SysDict::getCode, code)
                .eq(SysDict::getEnabled, 1));
        if (!exists) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    type.getLabel() + "选项不合法，请刷新页面后重试");
        }
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "成员不存在或已被删除");
        }
        return user;
    }

    private User currentUser() {
        User user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在或已被删除");
        }
        return user;
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
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
