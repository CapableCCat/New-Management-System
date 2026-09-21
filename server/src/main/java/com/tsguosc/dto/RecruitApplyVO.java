package com.tsguosc.dto;

import com.tsguosc.entity.RecruitApply;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核台的报名记录出参（字典字段保留 code，前端用 dict store 转中文）。
 */
public record RecruitApplyVO(
        Long id,
        String name,
        String phone,
        String college,
        String major,
        String majorText,
        List<String> intentDepartments,
        List<String> tags,
        String tagText,
        Integer gender,
        String province,
        String city,
        Integer status,
        String rejectReason,
        Long reviewerId,
        LocalDateTime reviewedAt,
        Long userId,
        LocalDateTime createdAt
) {

    public static RecruitApplyVO from(RecruitApply entity) {
        if (entity == null) {
            return null;
        }
        return new RecruitApplyVO(
                entity.getId(),
                entity.getName(),
                entity.getPhone(),
                entity.getCollege(),
                entity.getMajor(),
                entity.getMajorText(),
                entity.getIntentDepartments(),
                entity.getTags(),
                entity.getTagText(),
                entity.getGender(),
                entity.getProvince(),
                entity.getCity(),
                entity.getStatus(),
                entity.getRejectReason(),
                entity.getReviewerId(),
                entity.getReviewedAt(),
                entity.getUserId(),
                entity.getCreatedAt()
        );
    }
}
