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
 * 头像对象存储（MinIO，PRD F-007）。
 *
 * <p>要点：
 * <ul>
 *   <li>未配置凭据时**降级**：应用照常启动，上传接口返回友好错误（不因本地没起 MinIO 而启动失败）</li>
 *   <li>启动时确保 bucket 存在并设为**公开读**（见 {@link MinioSupport}）—— 头像是 {@code <img>} 直接加载的，带不了 token</li>
 *   <li>校验三层：扩展名 + 声明 Content-Type + **文件头魔数**，与公告配图共用 {@link ImageValidator}</li>
 *   <li>对象 key 形如 {@code avatars/{userId}/{时间戳}.{ext}}；换头像时删旧对象，不留孤儿文件</li>
 * </ul>
 */
@Slf4j
@Component
public class AvatarStorage {

    private static final DateTimeFormatter KEY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MinioProperties properties;
    private final MinioClient client;

    public AvatarStorage(MinioProperties properties) {
        this.properties = properties;
        this.client = MinioSupport.clientOrNull(properties);
    }

    /** 启动时建桶 + 设公开读策略（幂等；与公告配图共用 {@link MinioSupport}） */
    @PostConstruct
    public void init() {
        AvatarUrls.configure(properties.resolvePublicPrefix());
        MinioSupport.ensurePublicBucket(client, properties, log, "头像");
    }

    public boolean enabled() {
        return client != null;
    }

    /**
     * 上传头像。
     *
     * @return 对象 key（写库用），例如 {@code avatars/40/20260923120000123.png}
     */
    public String upload(Long userId, MultipartFile file) {
        if (client == null) {
            throw new BusinessException(ResultCode.DOWNSTREAM_ERROR, "头像上传暂不可用：对象存储未配置");
        }
        ImageValidator.ValidatedImage image = ImageValidator.validate(file, "头像");

        String key = "avatars/" + userId + "/" + LocalDateTime.now().format(KEY_TIME) + "." + image.ext();
        try (InputStream in = new ByteArrayInputStream(image.bytes())) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(in, image.bytes().length, -1)
                    .contentType(image.contentType())
                    .build());
        } catch (Exception e) {
            log.error("头像上传失败：userId={}, key={}", userId, key, e);
            throw new BusinessException(ResultCode.DOWNSTREAM_ERROR, "头像上传失败，请稍后重试");
        }
        return key;
    }

    /** 删除旧头像对象（失败只记日志，不影响业务） */
    public void delete(String stored) {
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
            log.warn("删除旧头像失败（忽略）：key={}, err={}", key, e.getMessage());
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
