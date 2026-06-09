package com.yky.blog.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;
    private String publicEndpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private long imageMaxSize;
    private long videoMaxSize;
    private List<String> allowedImageExtensions = new ArrayList<>();
    private List<String> allowedImageContentTypes = new ArrayList<>();
    private List<String> allowedVideoExtensions = new ArrayList<>();
    private List<String> allowedVideoContentTypes = new ArrayList<>();
}
