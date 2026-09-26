package com.tsguosc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 公告发布 / 编辑请求（PRD F-008：标题 + 富文本内容 + 是否置顶）。
 *
 * <p>长度上限与建表语句对齐：`title` VARCHAR(128)；`content` 是 MEDIUMTEXT，
 * 这里再设一个 10 万字符的护栏，防止超大请求把连接占满。
 */
public record AnnouncementSaveRequest(

        @NotBlank(message = "请填写公告标题")
        @Size(max = 128, message = "公告标题不能超过 128 个字符")
        String title,

        @NotBlank(message = "请填写公告内容")
        @Size(max = 100000, message = "公告内容过长，请精简后再提交")
        String content,

        /** 是否置顶；不传视为否 */
        Boolean isTop
) {
}
