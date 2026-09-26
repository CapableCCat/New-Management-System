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
 * 轻量反馈（表 feedback，T2 建表、T16 启用；PRD F-014）。
 *
 * <p>**提交免登录** —— 新生在报名结果态就可能提交，此时没有账号，
 * 因此 {@code created_by} 允许为空（审计填充在无登录上下文时会跳过，见 {@code MybatisPlusConfig}）。
 *
 * <p>{@code source} 取值见字典 {@code feedback_source}（1 报名成功页 / 2 成员端），
 * 只用于复盘时区分来源，不参与权限判断。
 *
 * <p>{@code handled} 是复盘用的处理进度：0 未处理 / 1 已处理。PRD 只要求"可查看列表"，
 * 这一列是社长确认加上的（清单 §6 D123），便于区分有没有看过了。
 */
@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 反馈内容（≤1000 字符，与建表语句一致） */
    private String content;

    /** 联系方式（选填，手机号 / 微信 / 邮箱都行） */
    private String contact;

    /** 来源 1 报名成功页 2 成员端（字典 feedback_source） */
    private Integer source;

    /** 处理状态 0 未处理 1 已处理 */
    private Integer handled;

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
