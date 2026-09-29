package com.tsguosc.dto;

/**
 * 一个孤儿对象（T26）。
 *
 * @param key          对象 key（如 {@code avatars/40/20260923120000.png}）
 * @param kind         人话化的类型（头像 / 公告配图 / 社团 Logo）
 * @param size         字节数
 * @param lastModified 最后修改时间（ISO-8601 字符串，可能为 null）
 */
public record StorageOrphanVO(String key, String kind, long size, String lastModified) {
}
