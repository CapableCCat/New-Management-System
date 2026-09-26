package com.tsguosc.dto;

/**
 * 导入成功后返回的账号（PRD F-009：一次性导出密码清单 = 姓名 + 手机号 + 初始密码）。
 *
 * <p>⚠️ {@code password} 是**明文初始密码**，遵循 T7 的同一口径（§6 D8）：
 * **不落库**、只出现在本次响应里，前端展示一次、可复制/下载后即散。
 */
public record ImportAccountVO(String name, String phone, String password) {
}
