package com.tsguosc.dto;

import java.util.List;

/**
 * 孤儿文件**扫描结果**（T26，dry-run）。
 *
 * <p>这一步**只读不删** —— 社长要求「必须先 dry-run 打印待删清单再执行」。
 *
 * @param bucket            桶名
 * @param totalObjects      受管前缀下的对象总数
 * @param referencedObjects 其中被引用（有主）的数量
 * @param orphanBytes       孤儿对象占用的总字节数
 * @param truncated         孤儿列表是否被截断（超过上限只返回前 N 条）
 * @param orphans           孤儿清单
 */
public record StorageScanVO(
        String bucket,
        int totalObjects,
        int referencedObjects,
        long orphanBytes,
        boolean truncated,
        List<StorageOrphanVO> orphans) {
}
