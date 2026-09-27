package com.tsguosc.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

import java.nio.charset.StandardCharsets;

/**
 * prod 下**彻底**关闭接口文档入口（清单 §6 D15）。
 *
 * <p>为什么要这个类：`knife4j.enable=false` + `springdoc.*.enabled=false` 只关掉了「接口数据」
 * （`/v3/api-docs` 会 404），但 knife4j 的 **`doc.html` 页面外壳与 webjars 静态资源仍在 jar 里**，
 * 由 Spring 的静态资源处理直接返回 200 —— 2026-09-26 T17 用 prod profile 冒烟时实测发现。
 * 页面本身拿不到接口数据（打开是空的），但 D15 写的是"prod 关闭 Knife4j"，所以这里让它名副其实。
 *
 * <p>响应体沿用项目口径（HTTP 200 + `code=40400`），与 `/v3/api-docs` 在 prod 下的表现一致。
 * 只在 prod 生效 —— dev 下文档照常可用。
 */
@Configuration
@Profile("prod")
public class ProdDocDisabledConfig {

    /** 需要拦掉的路径前缀/精确路径 */
    private static final String[] BLOCKED_PREFIXES = {"/webjars/", "/v3/api-docs", "/swagger-ui"};

    private static final String[] BLOCKED_EXACT = {"/doc.html", "/swagger-ui.html"};

    private static final String NOT_FOUND_BODY = "{\"code\":40400,\"message\":\"请求的资源不存在\",\"data\":null}";

    @Bean
    public FilterRegistrationBean<Filter> prodDocBlocker() {
        Filter filter = (request, response, chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            if (isBlocked(req.getRequestURI())) {
                HttpServletResponse res = (HttpServletResponse) response;
                res.setStatus(HttpServletResponse.SC_OK);
                res.setContentType("application/json;charset=UTF-8");
                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                res.getWriter().write(NOT_FOUND_BODY);
                return;
            }
            chain.doFilter(request, response);
        };
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        // 放在最前面：别让静态资源处理先碰到它
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    private static boolean isBlocked(String uri) {
        if (uri == null) {
            return false;
        }
        for (String exact : BLOCKED_EXACT) {
            if (exact.equals(uri)) {
                return true;
            }
        }
        for (String prefix : BLOCKED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
