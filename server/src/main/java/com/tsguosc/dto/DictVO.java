package com.tsguosc.dto;

import com.tsguosc.entity.SysDict;

/**
 * 字典条目出参。
 */
public record DictVO(
        Long id,
        String type,
        String code,
        String label,
        Integer sort,
        Integer enabled,
        String remark
) {

    public static DictVO from(SysDict entity) {
        if (entity == null) {
            return null;
        }
        return new DictVO(entity.getId(), entity.getType(), entity.getCode(), entity.getLabel(),
                entity.getSort(), entity.getEnabled(), entity.getRemark());
    }
}
