package com.tsguosc.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 纳新报名记录（表 recruit_apply，全量保留，审核状态只在本表维护）。
 *
 * <p>报名不建账号：审核通过（T7）之后才会往 user 表建号并回填 user_id。
 */
@Data
@TableName(value = "recruit_apply", autoResultMap = true)
public class RecruitApply {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 手机号：报名唯一标识（uk_apply_phone） */
    private String phone;

    /** 学院（sys_dict: college.code） */
    private String college;

    /** 专业（sys_dict: major.code） */
    private String major;

    /** 专业选「其他」时的手填值 */
    private String majorText;

    /** 意向部门（多选，code 数组字符串，如 ["1","3"]） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> intentDepartments;

    /** 兴趣标签（多选，code 数组字符串） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 兴趣标签选「其他（自由补充）」时的手填值 */
    private String tagText;

    /** 性别 0未填 1男 2女 */
    private Integer gender;

    private String province;

    private String city;

    /** 状态 0待审 1通过 2拒绝 */
    private Integer status;

    /** 拒绝原因（对外措辞） */
    private String rejectReason;

    private Long reviewerId;

    private LocalDateTime reviewedAt;

    /** 审核通过后回填的 user.id */
    private Long userId;

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
