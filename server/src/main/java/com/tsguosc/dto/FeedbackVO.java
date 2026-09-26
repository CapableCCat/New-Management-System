package com.tsguosc.dto;

import com.tsguosc.entity.Feedback;

import java.time.LocalDateTime;

/**
 * 反馈出参（管理端列表用）。
 *
 * <p>{@code source} 只回编码，中文文案由前端按字典 {@code feedback_source} 渲染 ——
 * 字典文案改了立即生效，不在后端写死。
 */
public record FeedbackVO(
        Long id,
        String content,
        String contact,
        Integer source,
        Integer handled,
        LocalDateTime createdAt
) {

    public static FeedbackVO from(Feedback entity) {
        if (entity == null) {
            return null;
        }
        return new FeedbackVO(
                entity.getId(),
                entity.getContent(),
                entity.getContact(),
                entity.getSource(),
                entity.getHandled(),
                entity.getCreatedAt()
        );
    }
}
