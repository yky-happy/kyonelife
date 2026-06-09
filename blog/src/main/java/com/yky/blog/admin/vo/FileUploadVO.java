package com.yky.blog.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadVO {

    private String url;
    private String objectName;
    private String originalFilename;
    private Long size;
}
