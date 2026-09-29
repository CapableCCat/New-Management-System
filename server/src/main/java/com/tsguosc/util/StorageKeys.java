package com.tsguosc.util;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对象存储的 key 约定与引用提取（T26 孤儿文件清理用）。
 *
 * <p>三类资产的 key 前缀（各自落在自己的 Storage 类里，这里汇总成**单一出处**，便于扫描与识别）：
 * <ul>
 *   <li>{@code avatars/}       头像      —— 由 {@link AvatarStorage} 生成</li>
 *   <li>{@code announcements/} 公告配图  —— 由 {@link AnnouncementImageStorage} 生成</li>
 *   <li>{@code club/}          社团 Logo —— 由 {@link ClubLogoStorage} 生成</li>
 * </ul>
 *
 * <p><b>为什么"只扫受管前缀"</b>：桶里理论上只该有这三类对象，但**万一有人手工往桶里放了别的东西**
 * （备份、临时文件），那不是本工具该删的 —— 扫描只考虑这三个前缀，范围外的对象一律不碰。
 *
 * <p><b>引用形态</b>（三类资产在库里存**都是对象 key**，不是完整 URL）：
 * <ul>
 *   <li>头像：{@code user.avatar_url}</li>
 *   <li>公告配图：{@code announcement.content} 富文本里的 {@code <img src="announcements/…">}
 *       —— 落库前由 {@link HtmlSanitizer} 把完整地址压成 key，所以这里直接按 key 匹配</li>
 *   <li>社团 Logo：{@code sys_config.club_logo}</li>
 * </ul>
 */
public final class StorageKeys {

    /** 受管的 key 前缀（扫描与删除都只在这个范围内进行） */
    public static final List<String> MANAGED_PREFIXES = List.of("avatars/", "announcements/", "club/");

    /** 从富文本里抽公告配图 key：{@code src="announcements/..."}（落库形态已是 key） */
    private static final Pattern ANNOUNCEMENT_IMG_SRC =
            Pattern.compile("src=\"(announcements/[^\"]+)\"");

    /** key 是否属于受管前缀 */
    public static boolean isManaged(String key) {
        return key != null && MANAGED_PREFIXES.stream().anyMatch(key::startsWith);
    }

    /** 人话化的类型名（给界面显示用） */
    public static String kindOf(String key) {
        if (key == null) {
            return "未知";
        }
        if (key.startsWith("avatars/")) {
            return "头像";
        }
        if (key.startsWith("announcements/")) {
            return "公告配图";
        }
        if (key.startsWith("club/")) {
            return "社团 Logo";
        }
        return "其他";
    }

    /** 从公告正文里抽出所有配图 key（去重） */
    public static void collectAnnouncementKeys(String content, Set<String> into) {
        if (content == null || content.isEmpty()) {
            return;
        }
        Matcher matcher = ANNOUNCEMENT_IMG_SRC.matcher(content);
        while (matcher.find()) {
            into.add(matcher.group(1));
        }
    }

    /**
     * 库里的值 → 对象 key（用于构建"被引用的 key"集合）。
     *
     * <p>正常情况库里存的就是 key；**历史数据可能直存完整 URL**（见 {@code AvatarUrls} 的兼容分支），
     * 所以这里也做一次还原：命中公开前缀就剥掉；命中的是别的地址就原样返回（会被后续的
     * "是否受管前缀"判断排除，等于不引用任何对象）。
     *
     * @return 归一化后的 key；空值返回 {@code null}
     */
    public static String toObjectKey(String stored, String publicPrefix) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        String value = stored.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            if (StringUtils.hasText(publicPrefix) && value.startsWith(publicPrefix + "/")) {
                return value.substring(publicPrefix.length() + 1);
            }
            return value;
        }
        return value;
    }

    private StorageKeys() {
    }
}
