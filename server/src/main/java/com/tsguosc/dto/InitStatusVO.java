package com.tsguosc.dto;

/**
 * 系统初始化状态（F-012：首次启动若无任何成员，则强制引导创建超管）。
 *
 * @param initialized user 表是否已有记录
 */
public record InitStatusVO(boolean initialized) {
}
