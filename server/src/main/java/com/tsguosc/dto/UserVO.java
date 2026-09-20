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
