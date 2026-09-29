package com.tsguosc.service;

import com.tsguosc.dto.StorageCleanResultVO;
import com.tsguosc.dto.StorageScanVO;

import java.util.List;

/**
 * 对象存储孤儿文件清理（T26）。
 *
 * <p>"孤儿" = **在受管前缀下、但没有任何数据引用**的对象。三类资产的引用来源：
 * 头像 → {@code user.avatar_url}；公告配图 → {@code announcement.content} 富文本；
 * 社团 Logo → {@code sys_config.club_logo}。
 *
 * <p>为什么是**手动触发**而不是定时任务（《V1.0 收尾需求》§5.2 的建议）：
 * 社团规模下不会有几个孤儿，定时任务反而多一个"半夜悄悄删东西"的不确定因素；
 * 手动版每次都能看到清单，可控。
 *
 * <p>⚠️ 已不再需要处理的一类：**社团 Logo** —— T20 起换图时会同步删旧对象，
 * 所以正常情况下不会再产生 Logo 孤儿（扫描仍覆盖这个前缀，兜底早期残留）。
 */
public interface StorageCleanupService {

    /**
     * **扫描**孤儿文件（dry-run，只读不删）。
     *
     * @return 扫描结果（含孤儿清单）
     */
    StorageScanVO scan();

    /**
     * **清理**指定的孤儿对象。
     *
     * <p>⚠️ 不信任传入的 key：每个 key 都会**重新校验**（受管前缀 + 当前仍是孤儿 + 桶里确实存在），
     * 校验不过的一律跳过并计入 {@code skipped} —— 防止"清单是旧的、期间对象又被引用了"而误删。
     *
     * @param keys 待删除的对象 key（来自上一次扫描结果）
     */
    StorageCleanResultVO clean(List<String> keys);
}
