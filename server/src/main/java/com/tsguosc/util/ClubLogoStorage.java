package com.tsguosc.util;

import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 社团 Logo 对象存储（MinIO，PRD F-001 第 1 步）。
 *
 * <p>与头像 / 公告配图的区别：
 * <ul>
 *   <li>Logo 是**全站唯一一份**，所以 key 固定前缀 {@code club/logo_时间戳.ext}，
 *       换 Logo 时**删掉旧对象**（避免桶里留孤儿——《清单》§7 `O10` 的 Logo 这一条就此解决）</li>
 *   <li>库里存**对象 key**（写进 {@code sys_config.club_logo}），输出时由
 *       {@code SysConfigServiceImpl} 拼 {@code MINIO_PUBLIC_URL} 前缀 —— 换域名不失效</li>
 *   <li>校验口径与另两处完全一致（jpg / png、≤2MB、扩展名 + Content-Type + 文件头魔数三层），
 *       复用 {@link ImageValidator}；桶与公开读策略复用 {@link MinioSupport}</li>
 *   <li>MinIO 未配置时**降级**：应用照常启动，上传返回友好错误（报名页只是不显示 Logo）</li>
 * </ul>
 */
@Slf4j
@Component
public class ClubLogoStorage {

    private static final DateTimeFormatter KEY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 对象 key 前缀：全站唯一一份 Logo，放在 club/ 下便于识别与清理 */
    private static final String KEY_PREFIX = "club/logo_";

    private final MinioProperties properties;
    private final MinioClient client;

    public ClubLogoStorage(MinioProperties properties) {
        this.properties = properties;
        this.client = MinioSupport.clientOrNull(properties);
    }

    /** 启动时建桶 + 设公开读策略（幂等；三处共用 {@link MinioSupport}） */
    @PostConstruct
    public void init() {
        MinioSupport.ensurePublicBucket(client, properties, log, "社团 Logo");
    }

    public boolean enabled() {
        return client != null;
    }

    /** 对象 key → 可访问地址（与报名页出参同一口径，前缀取自 {@code MINIO_PUBLIC_URL}） */
    public String publicUrl(String key) {
        return StringUtils.hasText(key) ? properties.resolvePublicPrefix() + "/" + key : null;
    }

    /**
     * 上传 Logo。
     *
     * @return 对象 key，形如 {@code club/logo_20260928163000000.png}
     */
    public String upload(MultipartFile file) {
        if (client == null) {
            throw new BusinessException(ResultCode.DOWNSTREAM_ERROR, "Logo 上传暂不可用：对象存储未配置");
        }
        ImageValidator.ValidatedImage image = ImageValidator.validate(file, "Logo");

        String key = KEY_PREFIX + LocalDateTime.now().format(KEY_TIME) + "." + image.ext();
        try (InputStream in = new ByteArrayInputStream(image.bytes())) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(in, image.bytes().length, -1)
                    .contentType(image.contentType())
                    .build());
        } catch (Exception e) {
            log.error("Logo 上传失败：key={}", key, e);
            throw new BusinessException(ResultCode.DOWNSTREAM_ERROR, "Logo 上传失败，请稍后重试");
        }
        return key;
    }

    /** 删除旧 Logo 对象（失败只记日志，不影响业务） */    public void delete(String stored) {
        if (client == null || !StringUtils.hasText(stored)) {
            return;
        }
        String key = toObjectKey(stored.trim());
        if (key == null) {
            return;
        }
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.warn("删除旧 Logo 失败（忽略）：key={}, err={}", key, e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    /** 把库里存的值（对象 key 或历史完整 URL）还原成对象 key */
    private String toObjectKey(String stored) {
        if (!stored.startsWith("http://") && !stored.startsWith("https://")) {
            return stored;
        }
        String prefix = properties.resolvePublicPrefix() + "/";
        if (stored.startsWith(prefix)) {
            return stored.substring(prefix.length());
        }
        // 非常规前缀：尝试按 bucket 名截断，截不到就放弃删除（避免误删）
        int idx = stored.indexOf("/" + properties.getBucket() + "/");
        return idx < 0 ? null : stored.substring(idx + properties.getBucket().length() + 2);
    }
}
