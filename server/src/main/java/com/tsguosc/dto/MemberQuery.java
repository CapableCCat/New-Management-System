package com.tsguosc.dto;

import lombok.Data;

/**
 * 成员档案 / 成员列表查询条件（GET 查询串绑定用，故用可变 POJO）。
 */
@Data
public class MemberQuery {

    /** 关键字：姓名 / 手机号 / 学号 模糊匹配 */
    private String keyword;

    /** 学院 code */
    private String college;

    /** 部门 code（部长会被后端强制覆盖为本部门） */
    private Integer department;

    /** 职位 0成员 1副部长 2部长 3社长 */
    private Integer duty;

    /** 状态 0正常 1冻结 */
    private Integer status;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 20;
}
