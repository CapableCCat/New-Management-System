package com.tsguosc.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告（表 announcement，T2 建表、T12 启用；PRD F-008）。
 *
 * <p>{@code content} 存**已清洗**的富文本 HTML（白名单见 {@code HtmlSanitizer}）；
 * 图片在其中以对象 key 形式存在，输出时再拼公开前缀。
 *
 * <p>{@code created_at} 即「发布时间」，列表排序恒为 {@code is_top DESC, created_at DESC}。
 * 表上没有单独的「草稿/发布」状态 —— PRD 只要求置顶，旧系统的「紧急公告」不沿用（§6 D81）。
 */
@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题（≤128 字符，与建表语句一致） */
    private String title;

    /** 富文本内容（已做 XSS 白名单清洗） */
    private String content;

    /** 置顶 0否 1是 */
    private Integer isTop;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic
    private Integer isDeleted;
}
