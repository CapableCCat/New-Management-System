package com.tsguosc.common.constant;

/**
 * Redis key 前缀（统一加 osc: 前缀，避免与 Sa-Token 自身的 key 冲突）。
 */
public final class RedisKeys {

    /** 图形验证码：osc:captcha:{captchaKey} → 答案，校验后立即删除 */
    public static final String CAPTCHA_PREFIX = "osc:captcha:";

    /** 登录失败计数：osc:login:fail:{phone} → 次数，TTL = 锁定时长 */
    public static final String LOGIN_FAIL_PREFIX = "osc:login:fail:";

    private RedisKeys() {
    }
}
