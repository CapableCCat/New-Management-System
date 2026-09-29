package com.tsguosc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.common.constant.ConfigKeys;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.config.MinioProperties;
import com.tsguosc.dto.StorageCleanResultVO;
import com.tsguosc.dto.StorageOrphanVO;
import com.tsguosc.dto.StorageScanVO;
import com.tsguosc.entity.Announcement;
import com.tsguosc.entity.SysConfig;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.AnnouncementMapper;
import com.tsguosc.mapper.SysConfigMapper;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.StorageCleanupService;
import com.tsguosc.util.MinioSupport;
import com.tsguosc.util.StorageKeys;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 对象存储孤儿文件清理实现（T26）。
 *
 * <p><b>安全设计的四条线</b>：
 * <ol>
 *   <li>**只扫受管前缀**（{@code avatars/ }{@code announcements/ }{@code club/}）——
 *       桶里万一有别人手工放的东西，一律不碰</li>
 *   <li>**先 dry-run 出清单**，删除必须显式传 key —— 用户看到的清单 = 被删的东西</li>
 *   <li>**删除前逐个复检**（受管前缀 + 当前仍是孤儿 + 桶里确实存在）——
 *       防"清单是旧的、期间对象又被引用了"</li>
 *   <li>**逻辑删除的行不算引用**（MyBatis-Plus 的 {@code @TableLogic} 会自动滤掉
 *       {@code is_deleted=1}）—— 这正是本工具要解决的问题：**删公告后配图/头像该能清掉**</li>
 * </ol>
 */
@Slf4j
@Service
public class StorageCleanupServiceImpl implements StorageCleanupService {

    /** 一次最多返回多少条孤儿（防一次拉爆接口；界面显示"已截断"提示） */
    private static final int MAX_ORPHANS = 500;

    private final MinioProperties properties;
    private final UserMapper userMapper;
    private final AnnouncementMapper announcementMapper;
    private final SysConfigMapper sysConfigMapper;

    /** MinIO 未配置时为 null（应用仍可启动，只是这个工具不可用） */
    private final MinioClient client;

    public StorageCleanupServiceImpl(
            MinioProperties properties,
            UserMapper userMapper,
            AnnouncementMapper announcementMapper,
            SysConfigMapper sysConfigMapper) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.announcementMapper = announcementMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.client = MinioSupport.clientOrNull(properties);
    }

    @Override
    public StorageScanVO scan() {
        requireStorage();
        Set<String> referenced = collectReferencedKeys();
        List<Item> managed = listManagedObjects();

        List<StorageOrphanVO> orphans = new ArrayList<>();
        long orphanBytes = 0;
        int referencedCount = 0;
        for (Item item : managed) {
            String key = item.objectName();
            if (referenced.contains(key)) {
                referencedCount++;
                continue;
            }
            long size = item.size();
            orphanBytes += Math.max(size, 0);
            if (orphans.size() < MAX_ORPHANS) {
                orphans.add(new StorageOrphanVO(
                        key,
                        StorageKeys.kindOf(key),
                        size,
                        item.lastModified() == null ? null : item.lastModified().toInstant().toString()));
            }
        }
        boolean truncated = orphans.size() < managed.size() - referencedCount;
        log.info("存储扫描：受管对象 {} 个，有引用 {} 个，孤儿 {} 个（截断={}）",
                managed.size(), referencedCount, orphans.size(), truncated);
        return new StorageScanVO(
                properties.getBucket(), managed.size(), referencedCount, orphanBytes, truncated, orphans);
    }

    @Override
    public StorageCleanResultVO clean(List<String> keys) {
        requireStorage();
        // 复检所需的两份数据（都是"此刻"的）
        Set<String> referenced = collectReferencedKeys();
        Set<String> existing = new HashSet<>();
        for (Item item : listManagedObjects()) {
            existing.add(item.objectName());
        }

        int deleted = 0;
        int skipped = 0;
        List<String> failed = new ArrayList<>();
        for (String raw : new LinkedHashSet<>(keys)) {
            String key = raw == null ? null : raw.trim();
            if (key == null || key.isEmpty()) {
                continue;
            }
            // 三重把关，任一不过就跳过（绝不删）
            if (!StorageKeys.isManaged(key)) {
                log.warn("跳过清理（不在受管前缀）：{}", key);
                skipped++;
                continue;
            }
            if (referenced.contains(key)) {
                log.warn("跳过清理（已被引用）：{}", key);
                skipped++;
                continue;
            }
            if (!existing.contains(key)) {
                // 桶里已经没有这个对象了（可能上一次已删）——不算失败
                log.info("跳过清理（对象已不存在）：{}", key);
                skipped++;
                continue;
            }
            try {
                client.removeObject(RemoveObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(key)
                        .build());
                deleted++;
            } catch (Exception e) {
                log.warn("清理失败：key={}, err={}", key, e.getMessage());
                failed.add(key);
            }
        }
        log.info("存储清理：删除 {} 个，跳过 {} 个，失败 {} 个", deleted, skipped, failed.size());
        return new StorageCleanResultVO(deleted, skipped, failed);
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    private void requireStorage() {
        if (client == null) {
            throw new BusinessException(ResultCode.DOWNSTREAM_ERROR, "对象存储未配置，无法执行存储维护");
        }
    }

    /** 收集"被引用的对象 key"（三类资产的单一出处） */
    private Set<String> collectReferencedKeys() {
        String prefix = properties.resolvePublicPrefix();
        Set<String> keys = new HashSet<>();

        // ① 头像（库里存 key；过滤空值）
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .select(User::getAvatarUrl)
                .isNotNull(User::getAvatarUrl)
                .ne(User::getAvatarUrl, ""));
        for (User user : users) {
            addIfKey(keys, StorageKeys.toObjectKey(user.getAvatarUrl(), prefix));
        }

        // ② 公告配图（正文里的 <img src="announcements/…">，落库形态已是 key）
        List<Announcement> announcements = announcementMapper.selectList(
                Wrappers.<Announcement>lambdaQuery().select(Announcement::getContent));
        for (Announcement announcement : announcements) {
            StorageKeys.collectAnnouncementKeys(announcement.getContent(), keys);
        }

        // ③ 社团 Logo（sys_config.club_logo）
        SysConfig logo = sysConfigMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, ConfigKeys.CLUB_LOGO)
                .select(SysConfig::getConfigValue));
        if (logo != null) {
            addIfKey(keys, StorageKeys.toObjectKey(logo.getConfigValue(), prefix));
        }
        return keys;
    }

    private void addIfKey(Set<String> keys, String key) {
        if (key != null && !key.isEmpty()) {
            keys.add(key);
        }
    }

    /** 列出受管前缀下的对象（范围外的对象不返回，也就永远不会被删） */
    private List<Item> listManagedObjects() {
        List<Item> managed = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = client.listObjects(ListObjectsArgs.builder()
                    .bucket(properties.getBucket())
                    .recursive(true)
                    .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (StorageKeys.isManaged(item.objectName())) {
                    managed.add(item);
                }
            }
        } catch (Exception e) {
            log.error("列举对象失败：bucket={}", properties.getBucket(), e);
            throw new BusinessException(ResultCode.DOWNSTREAM_ERROR, "读取对象存储失败，请稍后重试");
        }
        return managed;
    }
}
