package com.tsguosc.dto;

/**
 * 分布项（用于看板的学院 / 专业 / 性别 / 省份分布）。
 *
 * @param name  展示名（已是中文标签；省份为标准省名）
 * @param value 人数
 */
public record NameValueVO(String name, long value) {
}
