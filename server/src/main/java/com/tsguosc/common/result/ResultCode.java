package com.tsguosc.common.result;

import lombok.Getter;

/**
 * 全局业务/错误码。HTTP 状态码统一为 200，真实语义由 {@code code} 承载
 * （与旧系统约定保持一致，前端只需判断 code）。
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),

    /** 参数错误 */
    PARAM_ERROR(40000, "请求参数不正确"),

    /** 未登录或登录已过期 */
    UNAUTHORIZED(40100, "未登录或登录已过期"),

    /** 无权限 */
    FORBIDDEN(40300, "无权限访问"),

    /** 资源不存在 */
    NOT_FOUND(40400, "请求的资源不存在"),

    /** 系统异常 */
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试"),

    /** 下游服务不可用（Redis / MySQL / MinIO 等） */
    DOWNSTREAM_ERROR(50001, "下游服务不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
