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

    /**
     * 内部写入单个配置（**绕过 EDITABLE_KEYS 白名单**，供 Logo 这类二进制产物的键使用）。
     *
     * <p>⚠️ 不要从管理端请求直接调到这里 —— 白名单是「后台文本框能改哪些键」的唯一出处，
     * 本方法只给「上传/清除接口」这类自带校验的路径用。
     *
     * @param value 新值；传 {@code null} 表示清空（列置 NULL，而非空串）
     */
    void writeInternal(String key, String value);

    /** 公开：报名页需要的配置（报名开关 / 社团简介 / 审核时效文案 / Logo 地址） */
    RecruitInfoVO recruitInfo();
}
