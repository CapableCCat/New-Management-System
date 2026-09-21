package com.tsguosc.dto;

import com.tsguosc.entity.SysConfig;

/**
 * 系统配置出参（管理端）。
 */
public record ConfigVO(String key, String value, String remark, String updatedAt) {

    public static ConfigVO from(SysConfig entity) {
        if (entity == null) {
            return null;
        }
        return new ConfigVO(entity.getConfigKey(), entity.getConfigValue(), entity.getRemark(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString());
    }
}
