package com.tsguosc.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.tsguosc.common.constant.SessionKeys;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Objects;

/**
 * Sa-Token 鉴权配置（T4 起启用全局登录拦截器，见《开发任务点清单》§6 D10）。
 *
 * <p>拦截器做三件事：
 * <ol>
 *   <li>校验登录态（Header 里的 osc-token）</li>
 *   <li>冻结兜底：status=冻结 的账号一律拦下（T10 起，让"冻结"立即生效）</li>
 *   <li>首登强制改密兜底：未写 activated_at 的账号，除少量接口外一律拦下（不只靠前端跳转）</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    /** 账号被冻结（封禁 / 离社） */
    private static final int STATUS_FROZEN = 1;

    private final UserMapper userMapper;

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

                    // 2. 冻结兜底：档案里把成员设为「冻结」后，已登录的 token 立即失效
                    //    （登录入口本身也会拦，见 AuthServiceImpl；这里覆盖"冻结前已登录"的情况）
                    User current = userMapper.selectById(StpUtil.getLoginIdAsLong());
                    if (current == null) {
                        throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在或已被删除");
                    }
                    if (Objects.equals(current.getStatus(), STATUS_FROZEN)) {
                        throw new BusinessException(ResultCode.ACCOUNT_FROZEN);
                    }

                    // 3. 首登强制改密兜底
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
