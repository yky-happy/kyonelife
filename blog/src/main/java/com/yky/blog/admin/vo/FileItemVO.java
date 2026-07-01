package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "MinIO 文件项")
public class FileItemVO {
    private String objectName;
    private String url;
    private long size;
    private String lastModified;
}
