package com.opsflow.common.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置
 */
@Configuration
public class MinioConfig {

    @Value("${opsflow.minio.endpoint}")
    private String endpoint;

    @Value("${opsflow.minio.access-key}")
    private String accessKey;

    @Value("${opsflow.minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}