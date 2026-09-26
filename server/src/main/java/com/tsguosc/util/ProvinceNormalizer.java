package com.tsguosc.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 生源地省份归一化（T15 看板「地区分布」用）。
 *
 * <p>为什么要它：`user.province` / `recruit_apply.province` 是**报名时人手工填的自由文本**，
 * 实测库里同时存在「天津市」「Tianjin」「河北省」和空值 —— 不归一化就没法往地图上着墨。
 * 归一化在**后端**做（而不是前端）：单一出处，导出等其它统计也能复用同一套口径。
 *
 * <p>覆盖 34 个省级行政区（含**台湾省 / 香港特别行政区 / 澳门特别行政区**），
 * 支持：标准全名、简称、去掉后缀的写法、常见英文名。拿不准的一律返回 {@code null}，
 * 由调用方计入「未填写」，**不要瞎猜**（猜错在统计图上是看不出来的）。
 */
public final class ProvinceNormalizer {

    /** 别名 → 标准名（map 上的名字用标准全名） */
    private static final Map<String, String> ALIAS = new LinkedHashMap<>();

    /** 后缀，长的排前面（「特别行政区」必须先于「自治区」/「市」匹配） */
    private static final List<String> SUFFIXES = List.of(
            "特别行政区", "维吾尔自治区", "壮族自治区", "回族自治区", "自治区", "省", "市");

    private static void alias(String standard, String... names) {
        ALIAS.put(key(standard), standard);
        for (String name : names) {
            ALIAS.put(key(name), standard);
        }
    }

    private static String key(String value) {
        // 去空格与不可见字符、转小写、"中国"前缀一律去掉（中国香港 → 香港）
        String cleaned = value.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s\u00A0]+", "");
        return cleaned.startsWith("中国") ? cleaned.substring(2) : cleaned;
    }

    static {
        alias("北京市", "北京", "beijing");
        alias("天津市", "天津", "tianjin");
        alias("河北省", "河北", "hebei");
        alias("山西省", "山西", "shanxi");
        alias("内蒙古自治区", "内蒙古", "内蒙", "neimenggu", "innermongolia");
        alias("辽宁省", "辽宁", "liaoning");
        alias("吉林省", "吉林", "jilin");
        alias("黑龙江省", "黑龙江", "heilongjiang");
        alias("上海市", "上海", "shanghai");
        alias("江苏省", "江苏", "jiangsu");
        alias("浙江省", "浙江", "zhejiang");
        alias("安徽省", "安徽", "anhui");
        alias("福建省", "福建", "fujian");
        alias("江西省", "江西", "jiangxi");
        alias("山东省", "山东", "shandong");
        alias("河南省", "河南", "henan");
        alias("湖北省", "湖北", "hubei");
        alias("湖南省", "湖南", "hunan");
        alias("广东省", "广东", "guangdong");
        alias("广西壮族自治区", "广西", "guangxi");
        alias("海南省", "海南", "hainan");
        alias("重庆市", "重庆", "chongqing");
        alias("四川省", "四川", "sichuan");
        alias("贵州省", "贵州", "guizhou");
        alias("云南省", "云南", "yunnan");
        alias("西藏自治区", "西藏", "tibet", "xizang");
        alias("陕西省", "陕西", "shaanxi");
        alias("甘肃省", "甘肃", "gansu");
        alias("青海省", "青海", "qinghai");
        alias("宁夏回族自治区", "宁夏", "ningxia");
        alias("新疆维吾尔自治区", "新疆", "xinjiang");
        alias("台湾省", "台湾", "台湾地区", "taiwan");
        alias("香港特别行政区", "香港", "香港地区", "hongkong", "hk");
        alias("澳门特别行政区", "澳门", "澳门地区", "macao", "macau");
    }

    /**
     * 归一化为标准省名。
     *
     * @return 标准名；无法识别时返回 {@code null}（调用方计入「未填写」）
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String key = key(raw);
        if (key.isEmpty()) {
            return null;
        }
        String hit = ALIAS.get(key);
        if (hit != null) {
            return hit;
        }
        // 去掉行政区划后缀再试一次（例：「广西壮族自治区」已在别名表里，但「广西省」这种错填也能救）
        for (String suffix : SUFFIXES) {
            if (key.length() > suffix.length() && key.endsWith(suffix)) {
                hit = ALIAS.get(key.substring(0, key.length() - suffix.length()));
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    private ProvinceNormalizer() {
    }
}
