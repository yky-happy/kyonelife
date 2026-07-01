package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "网站配置编辑入参")
public class WebConfigDTO {
    private String siteName;
    private String logo;
    private String summary;
    private String author;
    private String authorAvatar;
    private String signature;
    private String github;
    private String email;
    private String aboutMe;
    private String icpNumber;
    private String bulletin;
}
