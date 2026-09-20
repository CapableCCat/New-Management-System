package com.tsguosc.common.constant;

/**
 * Sa-Token 会话（SaSession）里存放的自定义标记。
 */
public final class SessionKeys {

    /** 是否需要强制改密（首登未写 activated_at 时为 true） */
    public static final String NEED_CHANGE_PASSWORD = "needChangePassword";

    private SessionKeys() {
    }
}
