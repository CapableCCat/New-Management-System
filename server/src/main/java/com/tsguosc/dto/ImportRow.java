package com.tsguosc.dto;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入行模型（PRD F-009 模板：姓名 / 手机号 / 学号 / 学院 / 专业 / 部门 / 职位）。
 *
 * <p>本类只用于**写模板**（列头由注解生成，保证模板与校验用的是同一份列名）；
 * 读取上传文件时走的是 {@code Map<Integer, String>} 原始行，见 {@code MemberImportServiceImpl}
 * 的说明 —— 因为错误清单要精确到**行号**，自己按行遍历最好控。
 *
 * <p>列头常量单列出来，给导入服务做「列头必须与模板一致」的比对。
 */
@Data
public class ImportRow {

    public static final String COL_NAME = "姓名";
    public static final String COL_PHONE = "手机号";
    public static final String COL_STUDENT_ID = "学号";
    public static final String COL_COLLEGE = "学院";
    public static final String COL_MAJOR = "专业";
    public static final String COL_DEPARTMENT = "部门";
    public static final String COL_DUTY = "职位";

    @ExcelProperty(COL_NAME)
    private String name;

    @ExcelProperty(COL_PHONE)
    private String phone;

    @ExcelProperty(COL_STUDENT_ID)
    private String studentId;

    @ExcelProperty(COL_COLLEGE)
    private String college;

    @ExcelProperty(COL_MAJOR)
    private String major;

    @ExcelProperty(COL_DEPARTMENT)
    private String department;

    @ExcelProperty(COL_DUTY)
    private String duty;
}
