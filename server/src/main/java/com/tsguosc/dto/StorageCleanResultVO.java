package com.tsguosc.dto;

import java.util.List;

/**
 * 清理孤儿文件的结果（T26）。
 *
 * @param deleted 实际删除数
 * @param skipped 跳过数（请求里有、但复检发现「已被引用」或「不在受管前缀」的对象 —— 这类**一律不删**）
 * @param failed  删除失败的对象 key
 */
public record StorageCleanResultVO(int deleted, int skipped, List<String> failed) {
}
