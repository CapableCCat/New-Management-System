package com.tsguosc.config;

import cn.dev33.satoken.stp.StpInterface;
import com.tsguosc.common.constant.Roles;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sa-Token 角色提供者：把库里的 role / department / duty 推导成角色标识。
 *
 * <p>规则（PRD 第五章 + §6 D3）：
 * <ul>
 *   <li>role = 2 → super-admin（超管）</li>
 *   <li>department = 0 → leader-group（社长团，全社管理）</li>
 *   <li>duty = 2 → minister（部长，本部门管理）</li>
 *   <li>以上都没有 → member（普通成员）</li>
 * </ul>
 *
 * <p>V1.0 只用角色，不设细粒度权限（getPermissionList 返回空）。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private static final int ROLE_SUPER_ADMIN = 2;
    private static final int DEPARTMENT_LEADER_GROUP = 0;
    private static final int DUTY_MINISTER = 2;

    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return List.of();
        }
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null || Objects.equals(user.getStatus(), 1)) {
            return List.of();
        }
        List<String> roles = new ArrayList<>(3);
        if (Objects.equals(user.getRole(), ROLE_SUPER_ADMIN)) {
            roles.add(Roles.SUPER_ADMIN);
        }
        if (Objects.equals(user.getDepartment(), DEPARTMENT_LEADER_GROUP)) {
            roles.add(Roles.LEADER_GROUP);
        }
        if (Objects.equals(user.getDuty(), DUTY_MINISTER)) {
            roles.add(Roles.MINISTER);
        }
        if (roles.isEmpty()) {
            roles.add(Roles.MEMBER);
        }
        return roles;
    }
}
