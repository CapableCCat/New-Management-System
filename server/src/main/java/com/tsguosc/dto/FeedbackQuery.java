package com.tsguosc.dto;

import lombok.Data;

/**
 * 反馈列表查询条件（GET 查询串绑定用，故用可变 POJO）。
 */
@Data
public class FeedbackQuery {

    /** 来源 1 报名成功页 2 成员端；不传 = 全部 */
    private Integer source;

    /** 处理状态 0 未处理 1 已处理；不传 = 全部 */
    private Integer handled;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 20;
}
