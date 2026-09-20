package com.tsguosc.service;

import com.tsguosc.dto.DictCreateRequest;
import com.tsguosc.dto.DictTypeVO;
import com.tsguosc.dto.DictUpdateRequest;
import com.tsguosc.dto.DictVO;

import java.util.List;

/**
 * 字典管理（F-011，仅超管可写；公开读供全站引用）。
 */
public interface DictService {

    /** 全部字典类型元数据 */
    List<DictTypeVO> types();

    /** 公开读：某类型下**启用**的条目（报名页 / 查询页 / 成员端用） */
    List<DictVO> listEnabled(String type);

    /** 管理端读：某类型下全部条目（含停用） */
    List<DictVO> listAll(String type);

    /** 新增条目（type + code 唯一；编码入库后不可改） */
    void create(DictCreateRequest request);

    /** 编辑条目：只允许改文案 / 排序 / 备注 / 启停 */
    void update(Long id, DictUpdateRequest request);

    /** 上移 / 下移（direction = up | down） */
    void move(Long id, String direction);
}
