package com.tsguosc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OSC 社团管理系统后端启动类。
 *
 * <p>天津中德开源鸿蒙社（TSGU-OSC）· V1.0 纳新上线版。
 */
@SpringBootApplication
@MapperScan("com.tsguosc.mapper")
public class OscServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OscServerApplication.class, args);
    }
}
