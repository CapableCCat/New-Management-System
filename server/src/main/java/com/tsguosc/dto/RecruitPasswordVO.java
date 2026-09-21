package com.tsguosc.dto;

import java.util.List;

/**
 * 审核通过结果：**明文初始密码只在本次响应里出现一次，不落库**（PRD F-003）。
 *
 * @param successCount 建号成功的条数
 * @param failedCount  失败的条数
 * @param credentials  成功清单（姓名 / 手机号 / 初始密码），供展示与导出
 * @param failures     失败原因（形如"张三(13800000001)：该手机号已存在账号"）
 */
public record RecruitPasswordVO(
        int successCount,
        int failedCount,
        List<Credential> credentials,
        List<String> failures
) {

    public record Credential(String name, String phone, String password) {
    }

    public static RecruitPasswordVO single(String name, String phone, String password) {
        return new RecruitPasswordVO(1, 0, List.of(new Credential(name, phone, password)), List.of());
    }
}
