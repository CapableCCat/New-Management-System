package com.tsguosc.dto;

/**
 * 按日趋势点（看板「报名按日趋势」用）。
 *
 * @param date  日期（yyyy-MM-dd）
 * @param count 当日报名数（中间没有报名的日子补 0，曲线才连续）
 */
public record TrendPointVO(String date, long count) {
}
