package com.tsguosc.dto;

import java.util.List;

/**
 * Excel 导入结果（PRD F-009）。
 *
 * <p>逐行独立：合规的行已建号、不合规的行跳过并给出原因，**单行失败不影响其它行**。
 *
 * @param totalRows    非空明细行数（不含表头、不含空行）
 * @param successCount 建号成功数
 * @param failCount    跳过数
 * @param accounts     成功清单（含明文初始密码，只此一次）
 * @param errors       错误行清单（行号 + 原因）
 */
public record ImportResultVO(
        int totalRows,
        int successCount,
        int failCount,
        List<ImportAccountVO> accounts,
        List<ImportErrorVO> errors
) {
}
