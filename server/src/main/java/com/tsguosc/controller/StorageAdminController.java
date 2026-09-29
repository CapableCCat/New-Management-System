package com.tsguosc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tsguosc.common.constant.Roles;
import com.tsguosc.common.result.Result;
import com.tsguosc.dto.StorageCleanRequest;
import com.tsguosc.dto.StorageCleanResultVO;
import com.tsguosc.dto.StorageScanVO;
import com.tsguosc.service.StorageCleanupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 存储维护（T26）：对象存储孤儿文件清理。
 *
 * <p>**仅超管** —— 这是会真删东西的运维工具，不外放。
 *
 * <p>两个端点刻意分成「扫描」与「清理」两步：
 * <ul>
 *   <li>{@code GET /storage/admin/orphans} —— dry-run，**只读不删**，返回孤儿清单</li>
 *   <li>{@code POST /storage/admin/orphans/clean} —— 必须显式传 key 列表；
 *       服务端会逐个复检「是否仍是孤儿」，不是就跳过（防清单过期误删）</li>
 * </ul>
 */
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageAdminController {

    private final StorageCleanupService storageCleanupService;

    /** 扫描孤儿文件（dry-run，只读不删） */
    @GetMapping("/admin/orphans")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<StorageScanVO> scanOrphans() {
        return Result.ok(storageCleanupService.scan());
    }

    /** 清理指定的孤儿对象（key 来自上一次扫描结果；服务端会逐个复检） */
    @PostMapping("/admin/orphans/clean")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<StorageCleanResultVO> cleanOrphans(@Valid @RequestBody StorageCleanRequest request) {
        StorageCleanResultVO result = storageCleanupService.clean(request.keys());
        String message = "已清理 " + result.deleted() + " 个";
        if (result.skipped() > 0) {
            message += "，跳过 " + result.skipped() + " 个（期间已被引用或已不存在）";
        }
        if (!result.failed().isEmpty()) {
            message += "，失败 " + result.failed().size() + " 个";
        }
        return Result.ok(result, message);
    }
}
