package com.tsguosc.dto;

import java.time.LocalDateTime;

/**
 * 公告详情（成员端 / 管理端编辑弹窗共用）。
 *
 * <p>{@code content} 是**已清洗**的富文本 HTML（图片地址已由 key 拼成完整地址），
 * 前端渲染前还会用 DOMPurify 再清一遍（PRD F-008「前后端双重」）。
 */
public record AnnouncementDetailVO(
        Long id,
        String title,
        String content,
        Integer isTop,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
