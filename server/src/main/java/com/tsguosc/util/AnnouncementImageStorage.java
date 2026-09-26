package com.tsguosc.util;

import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 公告配图对象存储（MinIO，PRD F-008 富文本内容）。
 *
 * <p>与头像的区别：公告配图是**嵌在 HTML 里**的，所以本类对外返回**对象 key**
 * （{@code announcements/202609/xxx.png}），由 {@link HtmlSanitizer} 在落库/输出两端
 * 分别做「压成 key / 拼回地址」——库里不留完整地址，换域名才不失效。
 *
 * <p>校验口径与头像完全一致（jpg / png、≤2MB、扩展名 + Content-Type + 文件头魔数三层），
 * 复用 {@link ImageValidator}；桶与公开读策略复用 {@link MinioSupport}。
 */
@Slf4j
@Component
public class AnnouncementImageStorage {

    /** 目录按月份分片，避免单目录对象过多 */
    private static final DateTimeFormatter KEY_MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter KEY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MinioProperties properties;
    private final MinioClient client;

    public AnnouncementImageStorage(MinioProperties properties) {
        this.properties = properties;
        this.client = MinioSupport.clientOrNull(properties);
    }

    @PostConstruct
    public void init() {
        MinioSupport.ensurePublicBucket(client, properties, log, "公告配图");
    }

    public boolean enabled() {
        return client != null;
    }

    /**
     * 上传公告配图。
     *
     * @return 对象 key，形如 {@code announcements/202609/20260923143000123.png}
     */
    public String upload(MultipartFile file) {
        if (client == null) {
            throw new BusinessException(ResultCode.DOWNSTREAM_ERROR, "图片上传暂不可用：对象存储未配置");
        }
        ImageValidator.ValidatedImage image = ImageValidator.validate(file, "图片");

        String key = "announcements/" + LocalDate.now().format(KEY_MONTH) + "/"
                + LocalDateTime.now().format(KEY_TIME) + "." + image.ext();
        try (InputStream in = new ByteArrayInputStream(image.bytes())) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(in, image.bytes().length, -1)
                    .contentType(image.contentType())
                    .build());
        } catch (Exception e) {
            log.error("公告配图上传失败：key={}", key, e);
            throw new BusinessException(ResultCode.DOWNSTREAM_ERROR, "图片上传失败，请稍后重试");
        }
        return key;
    }

    /**
     * 对象 key → 可直接嵌进富文本的完整地址。
     *
     * <p>上传接口返回的是**完整地址**（编辑器要立刻预览），落库前再由
     * {@link HtmlSanitizer#cleanForStore(String)} 压回 key。
     */
    public String publicUrl(String key) {
        return properties.resolvePublicPrefix() + "/" + key;
    }
}
