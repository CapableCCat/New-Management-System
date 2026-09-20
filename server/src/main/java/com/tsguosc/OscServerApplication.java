package com.tsguosc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OSC 社团管理系统后端启动类。
 *
 * <p>天津中德开源鸿蒙社（TSGU-OSC）· V1.0 纳新上线版。
 * <p>T1 阶段仅提供 /health 健康检查；Mapper 扫描自 T2 起在 {@code com.tsguosc.mapper} 注册。
 */
@SpringBootApplication
public class OscServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OscServerApplication.class, args);
    }
}
