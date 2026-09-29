package com.tsguosc.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 清理孤儿文件的请求（T26）。
 *
 * <p>**必须显式传 key 列表**，而不是「删掉扫描到的全部」——
 * 这样"用户看到的清单"与"被删的东西"是同一份，不会因为两次扫描之间数据变了而误删。
 * 服务端仍会对每个 key **重新校验一次是否真是孤儿**（见 {@code StorageCleanupService#clean}）。
 *
 * @param keys 待删除的对象 key（来自上一次扫描结果）
 */
public record StorageCleanRequest(@NotEmpty(message = "请至少选择一个待清理的对象") List<String> keys) {
}
