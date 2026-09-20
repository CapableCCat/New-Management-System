package com.tsguosc.dto;

/**
 * 登录成功响应。
 *
 * @param tokenName          Header 名（osc-token），与后端 sa-token.token-name 一致
 * @param tokenValue         token 值，前端存本地并在后续请求 Header 里回传
 * @param needChangePassword 是否需要强制改密（activated_at 为空时为 true）
 * @param user               当前用户信息
 */
public record LoginVO(String tokenName, String tokenValue, boolean needChangePassword, UserVO user) {
}
