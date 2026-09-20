package com.tsguosc.common.constant;

import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 字典类型（PRD §6.4 + F-011，共 7 种，类型与编码口径由 PRD 固定）。
 *
 * <p>这是字典类型的**单一数据源**：接口会把本枚举下发给前端，前端不再硬编码类型中文名。
 */
@Getter
public enum DictType {

    DEPARTMENT("department", "部门", true),
    DUTY("duty", "职位", true),
    STATUS("status", "账号状态", true),
    COLLEGE("college", "学院", false),
    MAJOR("major", "专业", false),
    TAG("tag", "兴趣标签", false),
    FEEDBACK_SOURCE("feedback_source", "反馈来源", true);

    private final String code;
    private final String label;

    /**
     * 核心类型：编码由 PRD 固定（如部门 0~4、职位 0~3、状态 0~1），
     * 新增条目需谨慎；非核心类型（学院/专业/兴趣标签）可自由增补。
     */
    private final boolean core;

    DictType(String code, String label, boolean core) {
        this.code = code;
        this.label = label;
        this.core = core;
    }

    public static Optional<DictType> find(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(t -> t.code.equals(code.trim())).findFirst();
    }

    /** 取类型，非法则抛业务异常（参数错误） */
    public static DictType of(String code) {
        return find(code).orElseThrow(() ->
                new BusinessException(ResultCode.PARAM_ERROR, "未知的字典类型：" + code));
    }
}
