package com.tsguosc.util;

import com.tsguosc.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;

/**
 * MinIO 通用支撑（T11 头像 / T12 公告配图共用）。
 *
 * <p>只做两件与业务无关的事：建客户端、确保 bucket 存在且**公开读**。
 * 建桶逻辑本来长在 {@code AvatarStorage} 里，T12 要再加一处上传通道时抽出来 ——
 * 否则「同一份公开读策略」要维护两遍（项目的「单一出处」原则，见交接文档「五、全局铁律」第 3 条）。
 *
 * <p>为什么是公开读：头像与公告配图都是 {@code <img src>} 直接加载的，带不了 token，
 * 所以策略只放开 {@code s3:GetObject}，**不允许列举**。
 */
public final class MinioSupport {

    /** 按配置建客户端；凭据为空时返回 null（调用方据此降级，不让本地没起 MinIO 拖垮整个服务） */
    public static MinioClient clientOrNull(MinioProperties properties) {
        if (!properties.configured()) {
            return null;
        }
        return MinioClient.builder()
                .endpoint(properties.getEndpoint().trim())
                .credentials(properties.getAccessKey().trim(), properties.getSecretKey().trim())
                .build();
    }

    /**
     * 确保 bucket 存在并设为公开读（幂等，启动时调用）。
     *
     * <p>失败只记日志、不抛异常 —— 对象存储不可用只应影响对应上传功能。
     */
    public static void ensurePublicBucket(MinioClient client, MinioProperties properties, Logger log, String owner) {
        if (client == null) {
            log.warn("MinIO 未配置（MINIO_ACCESS_KEY / MINIO_SECRET_KEY 为空），{}上传功能不可用", owner);
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
            log.info("MinIO 就绪（{}）：endpoint={}, bucket={}, publicPrefix={}",
                    owner, properties.getEndpoint(), bucket, properties.resolvePublicPrefix());
        } catch (Exception e) {
            log.error("MinIO 初始化失败（{}上传将不可用）：{}", owner, e.getMessage());
        }
    }

    /** bucket 公开读策略：只放开 GetObject，不允许列举 */
    private static String publicReadPolicy(String bucket) {
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

    private MinioSupport() {
    }
}
