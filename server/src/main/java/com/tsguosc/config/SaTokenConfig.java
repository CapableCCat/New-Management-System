package com.tsguosc.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.tsguosc.common.constant.SessionKeys;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Sa-Token 鉴权配置（T4 起启用全局登录拦截器，见《开发任务点清单》§6 D10）。
 *
 * <p>拦截器做两件事：
 * <ol>
 *   <li>校验登录态（Header 里的 osc-token）</li>
 *   <li>首登强制改密兜底：未写 activated_at 的账号，除少量接口外一律拦下（不只靠前端跳转）</li>
 * </ol>
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /** 免登录白名单 */
    public static final String[] WHITE_LIST = {
            "/health",
            "/error",
            // 登录与初始化（初始化接口在业务层再判断"是否已初始化"）
            "/auth/captcha",
            "/auth/init-status",
            "/auth/init-admin",
            "/auth/login",
            // 字典公开读：/dict/types 与 /dict/{type}（两段路径）；/dict/admin/** 是三段，不在白名单内
            "/dict/*",
            // 公开报名：/recruit/info 与 /recruit/apply（两段路径），提交时仍需图形验证码
            "/recruit/*",
            // dev 接口文档（prod 下 knife4j/springdoc 已整体关闭）
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/favicon.ico"
    };

    /** 首登未改密时仍允许访问的接口 */
    private static final String[] NEED_CHANGE_PASSWORD_ALLOWED = {
            "/user/current",
            "/user/change-password",
            "/auth/logout"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 1. 登录态
                    StpUtil.checkLogin();

                    // 2. 首登强制改密兜底
                    Object needChange = StpUtil.getSession().get(SessionKeys.NEED_CHANGE_PASSWORD);
                    if (Boolean.TRUE.equals(needChange)) {
                        String path = SaHolder.getRequest().getRequestPath();
                        boolean allowed = Arrays.stream(NEED_CHANGE_PASSWORD_ALLOWED)
                                .anyMatch(path::startsWith);
                        if (!allowed) {
                            throw new BusinessException(ResultCode.NEED_CHANGE_PASSWORD);
                        }
                    }
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(WHITE_LIST);
    }
}
