package com.tsguosc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsguosc.entity.Announcement;

/**
 * 公告 Mapper（T12）。
 *
 * <p>全部查询都能用 LambdaQueryWrapper 表达（含「置顶优先 + 发布时间倒序」），
 * 故不写 XML。
 */
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
