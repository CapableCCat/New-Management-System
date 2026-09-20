package com.tsguosc.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置：审计字段自动填充（见《开发任务点清单》§6 D11）+ 内置分页插件。
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /** 分页插件（V1.0 体量用 MP 内置分页，不再引 PageHelper） */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /** 审计四字段自动填充：created_at / updated_at / created_by / updated_by */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
                Long userId = currentUserId();
                if (userId != null) {
                    strictInsertFill(metaObject, "createdBy", Long.class, userId);
                    strictInsertFill(metaObject, "updatedBy", Long.class, userId);
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                Long userId = currentUserId();
                if (userId != null) {
                    strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
                }
            }
        };
    }

    /** 当前登录用户 id；无登录上下文（如系统初始化）时返回 null */
    private static Long currentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId == null ? null : Long.valueOf(loginId.toString());
        } catch (Exception e) {
            log.debug("获取当前登录用户失败（忽略）：{}", e.getMessage());
            return null;
        }
    }
}
