package com.tsguosc.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.imageio.ImageIO;

/**
 * 关闭 ImageIO 的磁盘缓存（T17 上线前性能修复）。
 *
 * <p>背景（2026-09-26 T17 高峰并发模拟实测）：`/auth/captcha` 单发 **134ms**，
 * 而同样读库的 `/recruit/info` 只要 7.6ms —— 200 并发时 P95 达到 **8.6 秒**。
 * 逐层归因后的数据：
 * <pre>
 *   只画图（BufferedImage + drawString）        0.05 ms
 *   ＋ ImageIO.write（默认 useCache=true）     98.38 ms   ← 瓶颈在这里
 *   ＋ ImageIO.write（useCache=false，内存）    0.52 ms
 * </pre>
 * 也就是说：**不是 Nashorn、不是字体、不是验证码类型** ——
 * `ImageIO` 默认 `useCache=true` 时，往 OutputStream 写图会先在磁盘落一个临时文件
 * （Windows 上杀软/文件系统开销约 100ms/次）。190 倍的差距全在这一行配置上。
 *
 * <p>验证码是纳新期**第一个被打的接口**（新生扫码进报名页第一件事就是取图），
 * 所以这一项直接决定峰值下能不能扛住。全局关掉磁盘缓存即可（图像很小，走内存无压力）。
 *
 * <p>影响面：项目里除验证码库外没有别处直接用 ImageIO（头像 / 公告配图只做魔数校验，不解码），
 * 所以这个全局开关是安全的。
 */
@Slf4j
@Configuration
public class ImageIoConfig {

    @PostConstruct
    public void disableImageIoDiskCache() {
        ImageIO.setUseCache(false);
        log.info("ImageIO 磁盘缓存已关闭（验证码生成改走内存，避免每次 ~100ms 的临时文件开销）");
    }
}
