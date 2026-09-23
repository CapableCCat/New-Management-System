package com.tsguosc.dto;

import com.tsguosc.entity.User;

import java.time.LocalDateTime;

/**
 * 成员信息出参（绝不含密码）。
 */
public record UserVO(
        Long id,
        String phone,
        String name,
        String studentId,
        String college,
        String major,
        String majorText,
        Integer department,
        Integer duty,
        Integer role,
        Integer status,
        Integer gender,
        String province,
        String city,
        String avatarUrl,
        String bio,
        LocalDateTime activatedAt,
        LocalDateTime createdAt
) {

    /**
     * 屏蔽敏感列（手机号 / 学号）—— 供「成员」角色查看他人时使用。
     *
     * <p>见 PRD 第五章「敏感列可见性」：手机号与学号仅部长 / 社长团 / 超管可见；
     * 成员之间只可见基础列（姓名 / 部门 / 职位 / 学院 / 专业 / 个人简介 / 头像）。
     */
    public UserVO masked() {
        return new UserVO(id, null, name, null, college, major, majorText, department, duty, role,
                status, gender, province, city, avatarUrl, bio, activatedAt, createdAt);
    }

    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        return new UserVO(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getStudentId(),
                user.getCollege(),
                user.getMajor(),
                user.getMajorText(),
                user.getDepartment(),
                user.getDuty(),
                user.getRole(),
                user.getStatus(),
                user.getGender(),
                user.getProvince(),
                user.getCity(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getActivatedAt(),
                user.getCreatedAt()
        );
    }
}
