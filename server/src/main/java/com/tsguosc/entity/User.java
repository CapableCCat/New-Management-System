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
 * 正式社团成员（表 user，仅审核通过者；含本届与往届）。
 *
 * <p>字段与 `server/sql/01_schema.sql` 一一对应，新增字段请同步建表脚本。
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号：业务主键，全局唯一，同时是登录账号 */
    private String phone;

    /** BCrypt 密文 */
    private String password;

    private String name;

    /** 学号：可空，全局唯一 */
    private String studentId;

    /** 学院（sys_dict: college.code） */
    private String college;

    /** 专业（sys_dict: major.code） */
    private String major;

    /** 专业选「其他」时的手填值 */
    private String majorText;

    /** 部门 0社长团 1技术部 2运营部 3宣传部 4秘书处（可空，入社后分配） */
    private Integer department;

    /** 职位 0成员 1副部长 2部长 3社长 */
    private Integer duty;

    /** 角色 0普通成员 2超管 */
    private Integer role;

    /** 状态 0正常 1冻结（封禁/离社共用） */
    private Integer status;

    /** 性别 0未填 1男 2女 */
    private Integer gender;

    private String province;

    private String city;

    private String avatarUrl;

    /** 个人简介（富文本，渲染前清洗） */
    private String bio;

    /** 飞书 open_id（可空唯一；免登 Stretch） */
    private String feishuOpenId;

    /** 首登改密完成时间（NULL = 未激活 → 强制改密） */
    private LocalDateTime activatedAt;

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
