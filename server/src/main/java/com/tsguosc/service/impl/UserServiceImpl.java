package com.tsguosc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.ChangePasswordRequest;
import com.tsguosc.dto.UserVO;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.UserService;
import com.tsguosc.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 当前用户相关实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int STATUS_FROZEN = 1;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

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
}
