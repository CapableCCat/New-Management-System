package com.tsguosc.dto;

import java.time.LocalDateTime;

/**
 * 公告列表项（管理端列表 / 成员端列表 / 首页摘要共用）。
 *
 * <p>**不带 content** —— 富文本正文只在详情接口返回，避免列表把几十 KB 的 HTML 全塞进响应。
 * 摘要由后端从正文剥标签生成（见 {@code HtmlSanitizer.toSummary}）。
 */
public record AnnouncementVO(
        Long id,
        String title,
        String summary,
        Integer isTop,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
