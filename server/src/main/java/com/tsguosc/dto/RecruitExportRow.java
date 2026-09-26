package com.tsguosc.dto;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 报名/审核数据导出行（PRD F-013：`报名数据_YYYYMMDD.xlsx`）。
 *
 * <p>PRD 要求「含状态、拒绝原因、审核人、审核时间」这些**审核留痕**字段，
 * 故这是一份比成员名册更"复盘向"的档案：谁审的、什么时候审的、为什么拒，都在。
 * 末尾带上通过后回填的成员 id，便于从报名记录溯源到正式账号。
 */
@Data
public class RecruitExportRow {

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("学院")
    private String college;

    @ExcelProperty("专业")
    private String major;

    @ExcelProperty("意向部门")
    private String intentDepartments;

    @ExcelProperty("兴趣标签")
    private String tags;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("生源省")
    private String province;

    @ExcelProperty("生源市")
    private String city;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("拒绝原因")
    private String rejectReason;

    @ExcelProperty("审核人")
    private String reviewerName;

    @ExcelProperty("审核时间")
    private String reviewedAt;

    @ExcelProperty("提交时间")
    private String createdAt;

    @ExcelProperty("关联成员ID")
    private Long userId;
}
