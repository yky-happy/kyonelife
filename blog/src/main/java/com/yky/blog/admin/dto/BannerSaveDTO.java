package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "轮播图保存入参")
public class BannerSaveDTO {

    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    private String linkUrl;
    private String title;
    private Integer sort;
    @Schema(description = "状态：0=禁用 1=启用")
    private Integer status;
}
