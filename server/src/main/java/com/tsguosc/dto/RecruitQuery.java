package com.tsguosc.dto;

import lombok.Data;

/**
 * 审核台列表查询条件（GET 查询串绑定用，故用可变 POJO）。
 */
@Data
public class RecruitQuery {

    /** 状态 0待审 1通过 2拒绝；不传 = 全部 */
    private Integer status;

    /** 学院 code */
    private String college;

    /** 意向部门 code（部长会被后端强制覆盖为本部门） */
    private String department;

    /** 关键字：姓名或手机号模糊匹配 */
    private String keyword;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 20;
}
