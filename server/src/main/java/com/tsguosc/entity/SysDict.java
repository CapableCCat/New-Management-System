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
 * 字典（表 sys_dict）。
 *
 * <p>约定：**只启停（enabled）不删除**，保证 UNIQUE(type, code) 稳定；
 * 核心类型的已有 code 不可修改（见《开发任务点清单》§6 D19）。
 */
@Data
@TableName("sys_dict")
public class SysDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型：department / duty / status / college / major / tag / feedback_source */
    private String type;

    /** 编码（核心条目不可修改） */
    private String code;

    /** 文案（可修改） */
    private String label;

    /** 排序（升序） */
    private Integer sort;

    /** 启用 0停用 1启用 */
    private Integer enabled;

    private String remark;

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
