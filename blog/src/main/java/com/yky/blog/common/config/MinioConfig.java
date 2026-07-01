package com.yky.blog.common.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    /**
     * 仅构建客户端，不发起任何网络调用（build() 不连接 MinIO）。
     * 桶的存在性检查/创建/策略设置移到应用就绪后由 {@code MinioBucketInitializer} 容错执行，
     * 这样 MinIO 暂时不可用时不会阻塞应用启动。
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
