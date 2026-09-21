package com.tsguosc.service;

import com.tsguosc.dto.ConfigUpdateRequest;
import com.tsguosc.dto.ConfigVO;
import com.tsguosc.dto.RecruitInfoVO;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置读写（报告页展示 + 超管后台维护）。
 */
public interface SysConfigService {

    /** 取单个配置，不存在返回 empty */
    Optional<String> find(String key);

    /** 取单个配置，不存在返回默认值 */
    String getOrDefault(String key, String defaultValue);

    /** 管理端：列出全部可读配置（含键、值、备注、更新时间） */
    List<ConfigVO> list();

    /** 管理端：更新单个配置（仅允许改 ConfigKeys.EDITABLE_KEYS 里的键） */
    void update(ConfigUpdateRequest request);

    /** 公开：报名页需要的三项配置（报名开关 / 社团简介 / 审核时效文案） */
    RecruitInfoVO recruitInfo();
}
