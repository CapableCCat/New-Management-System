package com.tsguosc.dto;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 成员名册导出行（PRD F-013：`成员名册_YYYYMMDD.xlsx`）。
 *
 * <p>列口径（2026-09-26 与社长确认）：
 * 姓名 / 手机号 / 学号 / 学院 / 专业 / 部门 / 职位 / 性别 / 状态 / 生源省 / 生源市 / 加入时间。
 * **不含个人简介**（长文本会把表格撑变形）、不含头像地址与密码。
 *
 * <p>导出权限本身已限「社长团 / 超管」（他们本来就能看手机号与学号），
 * 所以这里不做列裁剪；哪天放宽给部长，就要按 {@code UserVO.masked()} 的口径去列。
 */
@Data
public class MemberExportRow {

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("学号")
    private String studentId;

    @ExcelProperty("学院")
    private String college;

    @ExcelProperty("专业")
    private String major;

    @ExcelProperty("部门")
    private String department;

    @ExcelProperty("职位")
    private String duty;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("生源省")
    private String province;

    @ExcelProperty("生源市")
    private String city;

    @ExcelProperty("加入时间")
    private String createdAt;
}
