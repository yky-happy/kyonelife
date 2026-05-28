package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "文章状态更新入参")
public class ArticleStatusDTO {

    @Schema(description = "状态：0=草稿 1=已发布 2=已下架", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文章状态不能为空")
    private Integer status;
}
