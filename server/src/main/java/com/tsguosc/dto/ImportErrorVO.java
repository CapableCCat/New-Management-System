package com.tsguosc.dto;

/**
 * 导入错误行（PRD F-009：错误行清单包含行号与原因）。
 *
 * @param row    Excel 里的真实行号（表头是第 1 行，明细从第 2 行开始）
 * @param reason 该行被跳过的原因（面向填表人，尽量写清怎么改）
 */
public record ImportErrorVO(int row, String reason) {
}
