package com.tsguosc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.common.constant.DictType;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.ChangePasswordRequest;
import com.tsguosc.dto.ProfileUpdateRequest;
import com.tsguosc.dto.StudentIdUpdateRequest;
import com.tsguosc.dto.UserVO;
import com.tsguosc.entity.SysDict;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.SysDictMapper;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.UserService;
import com.tsguosc.util.AvatarStorage;
import com.tsguosc.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 当前用户相关实现（F-007 个人中心）。
 *
 * <p>共用一条红线：**姓名、手机号不可自助修改** —— 它们不在任何自助请求体里，
 * 想改只能由干部走成员档案（T10）。学号走单独接口且「仅空可补录」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int STATUS_FROZEN = 1;
    private static final String CODE_OTHER = "other";

    private final UserMapper userMapper;
    private final SysDictMapper sysDictMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AvatarStorage avatarStorage;

    @Override
    public UserVO current() {
        return UserVO.from(requireCurrentUser());
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = requireCurrentUser();

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "旧密码错误");
        }
        passwordPolicy.validate(request.newPassword(), user.getPhone(), request.oldPassword());

        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(request.newPassword()));
        if (user.getActivatedAt() == null) {
            // 首登改密完成标记（之后不再覆盖，语义是"首次激活时间"）
            update.setActivatedAt(LocalDateTime.now());
        }
        userMapper.updateById(update);

        // 改密后强制登出，要求用新密码重新登录（PRD F-007）
        StpUtil.logout(user.getId());
        log.info("修改密码成功并强制登出：userId={}", user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(ProfileUpdateRequest request) {
        User user = requireCurrentUser();

        String college = trimToNull(request.college());
        String major = trimToNull(request.major());
        String majorText = trimToNull(request.majorText());
        if (major == null || !CODE_OTHER.equalsIgnoreCase(major)) {
            majorText = null;
        } else if (majorText == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "选择「其他」专业时，请填写具体专业名称");
        }

        // 字典编码合法性（与报名 / 成员档案同一套口径）
        if (college != null) {
            requireEnabledDict(DictType.COLLEGE, college);
        }
        if (major != null) {
            requireEnabledDict(DictType.MAJOR, major);
        }

        // ⚠️ 显式 set：整体提交语义下「清空专业 / 生源地 / 简介」都是合法操作，
        // 走 updateById 会被 MyBatis-Plus 忽略 null 而静默失败
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getCollege, college)
                .set(User::getMajor, major)
                .set(User::getMajorText, majorText)
                .set(User::getGender, request.gender() == null ? user.getGender() : request.gender())
                .set(User::getProvince, trimToNull(request.province()))
                .set(User::getCity, trimToNull(request.city()))
                .set(User::getBio, trimToNull(request.bio()))
                .set(User::getUpdatedBy, user.getId()));

        log.info("个人资料已更新：userId={}", user.getId());
        return UserVO.from(requireCurrentUser());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateStudentId(StudentIdUpdateRequest request) {
        User user = requireCurrentUser();
        if (StringUtils.hasText(user.getStudentId())) {
            // 仅空可补录：学号是评优与学校系统对接的验证依据，不允许反复自助改动
            throw new BusinessException(ResultCode.PARAM_ERROR, "账号已有学号，如需修改请联系管理员");
        }

        String studentId = request.studentId().trim();
        boolean taken = userMapper.exists(Wrappers.<User>lambdaQuery()
                .eq(User::getStudentId, studentId)
                .ne(User::getId, user.getId()));
        if (taken) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该学号已被其他成员使用");
        }

        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getStudentId, studentId)
                .set(User::getUpdatedBy, user.getId()));

        log.info("学号自助补录：userId={}", user.getId());
        return UserVO.from(requireCurrentUser());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO uploadAvatar(MultipartFile file) {
        User user = requireCurrentUser();
        String oldKey = user.getAvatarUrl();

        String newKey = avatarStorage.upload(user.getId(), file);

        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getAvatarUrl, newKey)
                .set(User::getUpdatedBy, user.getId()));

        // 换头像后删旧对象，避免桶里堆孤儿文件
        if (StringUtils.hasText(oldKey) && !Objects.equals(oldKey, newKey)) {
            avatarStorage.delete(oldKey);
        }

        log.info("头像已更新：userId={}, key={}", user.getId(), newKey);
        return UserVO.from(requireCurrentUser());
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    /** 取当前登录用户，并做存在性/冻结校验 */
    private User requireCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在或已被删除");
        }
        if (Objects.equals(user.getStatus(), STATUS_FROZEN)) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN);
        }
        return user;
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
