package com.yky.blog.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * MinIO 桶初始化器：在应用就绪后执行桶的存在性检查/创建/公读策略设置，并对异常容错。
 *
 * <p>放在 {@link ApplicationReadyEvent} 而非 Bean 创建时，且整段 try-catch：
 * 这样 MinIO 暂时不可用时只记录告警、不影响应用启动；待 MinIO 恢复后文件功能可用
 * （恢复后若桶仍不存在，首次上传会失败并提示，可重启或人工建桶）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void initBucket() {
        String bucket = minioProperties.getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(publicReadPolicy(bucket))
                    .build());
            log.info("MinIO 桶初始化完成: bucket={}", bucket);
        } catch (Exception e) {
            log.warn("MinIO 初始化失败（endpoint={}），文件上传功能暂不可用，待 MinIO 恢复后重试: {}",
                    minioProperties.getEndpoint(), e.getMessage());
        }
    }

    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }
}
