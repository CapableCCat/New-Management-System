package com.tsguosc.util;

import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * 头像对象存储（MinIO，PRD F-007）。
 *
 * <p>要点：
 * <ul>
 *   <li>未配置凭据时**降级**：应用照常启动，上传接口返回友好错误（不因本地没起 MinIO 而启动失败）</li>
 *   <li>启动时确保 bucket 存在并设为**公开读** —— 头像是 {@code <img>} 直接加载的，带不了 token</li>
 *   <li>校验三层：扩展名 + 声明 Content-Type + **文件头魔数**（Content-Type 是客户端可控的，不能只信它）</li>
 *   <li>对象 key 形如 {@code avatars/{userId}/{时间戳}.{ext}}；换头像时删旧对象，不留孤儿文件</li>
 * </ul>
 */
@Slf4j
@Component
public class AvatarStorage {

    /** PRD F-007：jpg/png，≤2MB */
    private static final long MAX_SIZE = 2L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_EXT = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png");

    private static final DateTimeFormatter KEY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MinioProperties properties;
    private final MinioClient client;

    public AvatarStorage(MinioProperties properties) {
        this.properties = properties;
        this.client = properties.configured()
                ? MinioClient.builder()
                        .endpoint(properties.getEndpoint().trim())
                        .credentials(properties.getAccessKey().trim(), properties.getSecretKey().trim())
                        .build()
                : null;
    }

    /** 启动时建桶 + 设公开读策略（幂等） */
    @PostConstruct
    public void init() {
        AvatarUrls.configure(properties.resolvePublicPrefix());
        if (client == null) {
            log.warn("MinIO 未配置（MINIO_ACCESS_KEY / MINIO_SECRET_KEY 为空），头像上传功能不可用");
            return;
        }
        String bucket = properties.getBucket();
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("已创建 MinIO bucket：{}", bucket);
            }
            client.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(publicReadPolicy(bucket))
                    .build());
            log.info("MinIO 就绪：endpoint={}, bucket={}, publicPrefix={}",
                    properties.getEndpoint(), bucket, properties.resolvePublicPrefix());
        } catch (Exception e) {
            // 不阻塞启动：MinIO 不可用时只影响头像功能
            log.error("MinIO 初始化失败（头像上传将不可用）：{}", e.getMessage());
        }
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
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "头像大小不能超过 2MB");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "头像仅支持 jpg / png 格式");
        }
        String declaredType = file.getContentType();
        if (StringUtils.hasText(declaredType) && !ALLOWED_EXT.get(ext).equalsIgnoreCase(declaredType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "头像格式与文件类型不一致，请重新选择图片");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "读取上传文件失败，请重试");
        }
        if (!matchesMagic(bytes, ext)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件内容不是有效的图片，请重新选择");
        }

        String key = "avatars/" + userId + "/" + LocalDateTime.now().format(KEY_TIME) + "." + ext;
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(in, bytes.length, -1)
                    .contentType(ALLOWED_EXT.get(ext))
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

    private String extensionOf(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXT.containsKey(ext) ? ext : null;
    }

    /** 文件头魔数校验：JPEG = FF D8 FF，PNG = 89 50 4E 47 0D 0A 1A 0A */
    private boolean matchesMagic(byte[] bytes, String ext) {
        if ("png".equals(ext)) {
            byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            if (bytes.length < magic.length) {
                return false;
            }
            for (int i = 0; i < magic.length; i++) {
                if (bytes[i] != magic[i]) {
                    return false;
                }
            }
            return true;
        }
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    /** 把库里存的值（对象 key 或历史完整 URL）还原成对象 key */
    private String toObjectKey(String stored) {
        if (!stored.startsWith("http://") && !startsWithHttps(stored)) {
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

    private boolean startsWithHttps(String value) {
        return value.startsWith("https://");
    }

    /** bucket 公开读策略：只放开 GetObject，不允许列举 */
    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }
}
