package com.tsguosc.util;

import org.springframework.util.StringUtils;

/**
 * 头像地址解析。
 *
 * <p>库里 `user.avatar_url` 存的是**对象 key**（如 {@code avatars/40/20260923120000.png}），
 * 对外输出时拼上已配置的公开前缀变成可访问 URL —— 这样上线换域名/端口时旧头像不会失效（§6 D75）。
 *
 * <p>用静态持有前缀而不用注入：需要拼地址的地方分散在登录（auth）、个人中心（user）、
 * 成员档案（member）三处 VO 构建点，注入解析器会把签名搞得很啰嗦；前缀只在启动时配置一次，运行期只读。
 */
public final class AvatarUrls {

    private static volatile String publicPrefix = "";

    /** 由 MinioProperties 在启动时调用一次 */
    public static void configure(String prefix) {
        publicPrefix = prefix == null ? "" : prefix;
    }

    /** 对象 key → 可访问地址；空值返回 null；历史数据里直存的完整 URL 原样返回 */
    public static String resolve(String stored) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        String value = stored.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return publicPrefix.isEmpty() ? value : publicPrefix + "/" + value;
    }

    private AvatarUrls() {
    }
}
