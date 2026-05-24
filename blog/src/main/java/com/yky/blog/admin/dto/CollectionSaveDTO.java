package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "合集新增、编辑入参")
public class CollectionSaveDTO {

    @Schema(description = "合集名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "合集名称不能为空")
    @Size(max = 50, message = "名称最多50个字符")
    private String name;

    @Schema(description = "封面图片地址")
    private String cover;

    @Schema(description = "合集简介")
    @Size(max = 200, message = "简介最多200个字符")
    private String description;

    @Schema(description = "排序权重，数字越小越靠前")
    private Integer sort;
}
