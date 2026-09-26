package com.tsguosc.dto;

import lombok.Data;

/**
 * 公告列表查询条件（GET 查询串绑定用，故用可变 POJO）。
 */
@Data
public class AnnouncementQuery {

    /** 标题关键字（可选，管理端检索用） */
    private String keyword;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;
}
