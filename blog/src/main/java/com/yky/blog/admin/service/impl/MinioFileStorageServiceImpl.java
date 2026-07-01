package com.yky.blog.admin.service.impl;

import com.yky.blog.admin.service.FileStorageService;
import com.yky.blog.admin.vo.FileUploadVO;
import com.yky.blog.common.config.MinioProperties;
import com.yky.blog.common.exception.BizException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MinioFileStorageServiceImpl implements FileStorageService {

    private static final DateTimeFormatter PATH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Set<String> IMAGE_DIRS = Set.of("article", "collection", "banner", "avatar", "config");
    private static final Set<String> VIDEO_DIRS = Set.of("article", "video");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public FileUploadVO uploadImage(MultipartFile file, String dir) {
        validateFile(
                file,
                minioProperties.getImageMaxSize(),
                minioProperties.getAllowedImageExtensions(),
                minioProperties.getAllowedImageContentTypes(),
                "图片",
                "仅支持 jpg、jpeg、png、webp、gif 格式图片"
        );
        String safeDir = normalizeDir(dir, IMAGE_DIRS, "article");
        return upload(file, safeDir);
    }

    @Override
    public FileUploadVO uploadVideo(MultipartFile file, String dir) {
        validateFile(
                file,
                minioProperties.getVideoMaxSize(),
                minioProperties.getAllowedVideoExtensions(),
                minioProperties.getAllowedVideoContentTypes(),
                "视频",
                "仅支持 mp4、webm、mov 格式视频"
        );
        String safeDir = normalizeDir(dir, VIDEO_DIRS, "video");
        return upload(file, safeDir);
    }

    private FileUploadVO upload(MultipartFile file, String safeDir) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String objectName = buildObjectName(safeDir, extension);

        try (java.io.InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectName)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new BizException("图片上传失败，请稍后重试");
        }

        String url = buildUrl(objectName);
        return new FileUploadVO(url, objectName, originalFilename, file.getSize());
    }

    @Override
    public java.util.List<com.yky.blog.admin.vo.FileItemVO> listFiles(String dir, int limit) {
        int safe = Math.min(Math.max(limit, 1), 500);
        String prefix = StringUtils.hasText(dir) ? dir + "/" : "";
        java.util.List<com.yky.blog.admin.vo.FileItemVO> items = new java.util.ArrayList<>();
        try {
            Iterable<io.minio.Result<io.minio.messages.Item>> objects = minioClient.listObjects(
                    io.minio.ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .prefix(prefix)
                            .recursive(true)
                            .maxKeys(safe)
                            .build());
            for (io.minio.Result<io.minio.messages.Item> r : objects) {
                io.minio.messages.Item item = r.get();
                if (item.isDir()) {
                    continue;
                }
                items.add(new com.yky.blog.admin.vo.FileItemVO(
                        item.objectName(),
                        buildUrl(item.objectName()),
                        item.size(),
                        item.lastModified() == null ? null : item.lastModified().toString()));
            }
        } catch (Exception e) {
            throw new BizException("获取文件列表失败");
        }
        return items;
    }

    @Override
    public void deleteFile(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new BizException("objectName 不能为空");
        }
        try {
            minioClient.removeObject(io.minio.RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new BizException("删除文件失败");
        }
    }

    private void validateFile(MultipartFile file,
                              long maxSize,
                              java.util.List<String> allowedExtensionsConfig,
                              java.util.List<String> allowedContentTypes,
                              String label,
                              String extensionErrorMessage) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的" + label);
        }
        if (file.getSize() > maxSize) {
            throw new BizException("%s大小不能超过 %sMB".formatted(label, maxSize / 1024 / 1024));
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        Set<String> allowedExtensions = allowedExtensionsConfig
                .stream()
                .map(item -> item.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowedExtensions.contains(extension)) {
            throw new BizException(extensionErrorMessage);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !allowedContentTypes.contains(contentType)) {
            throw new BizException(label + "类型不合法");
        }
    }

    private String normalizeDir(String dir, Set<String> allowedDirs, String defaultDir) {
        if (!StringUtils.hasText(dir)) {
            return defaultDir;
        }
        String normalized = dir.toLowerCase(Locale.ROOT).trim();
        if (!allowedDirs.contains(normalized)) {
            throw new BizException("上传目录不合法");
        }
        return normalized;
    }

    private String getExtension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        if (!StringUtils.hasText(extension)) {
            throw new BizException("图片文件缺少后缀名");
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    private String buildObjectName(String dir, String extension) {
        String datePath = LocalDate.now().format(PATH_DATE_FORMATTER);
        return "%s/%s/%s.%s".formatted(dir, datePath, UUID.randomUUID(), extension);
    }

    private String buildUrl(String objectName) {
        String endpoint = StringUtils.hasText(minioProperties.getPublicEndpoint())
                ? minioProperties.getPublicEndpoint()
                : minioProperties.getEndpoint();
        return endpoint.replaceAll("/+$", "") + "/" + minioProperties.getBucket() + "/" + objectName;
    }
}
