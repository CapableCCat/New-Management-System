package com.tsguosc.dto;

/**
 * 字典类型元数据（供前端渲染类型切换与管理页表头）。
 *
 * @param code  类型编码（department / college ...）
 * @param label 类型中文名
 * @param core  是否核心类型（编码由 PRD 固定）
 */
public record DictTypeVO(String code, String label, boolean core) {
}
