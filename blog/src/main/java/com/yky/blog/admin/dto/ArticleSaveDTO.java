package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "文章新增、编辑入参")
public class ArticleSaveDTO {

    @Schema(description = "文章标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题最多200个字符")
    private String title;

    @Schema(description = "封面图地址")
    @Size(max = 500, message = "封面图地址最多500个字符")
    private String cover;

    @Schema(description = "摘要")
    @Size(max = 500, message = "摘要最多500个字符")
    private String summary;

    @Schema(description = "HTML 内容")
    private String content;

    @Schema(description = "Markdown 原文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Markdown 原文不能为空")
    private String contentMd;

    @Schema(description = "SEO 关键词")
    @Size(max = 200, message = "SEO 关键词最多200个字符")
    private String keywords;

    @Schema(description = "AI 简短描述")
    private String aiDescribe;

    @Schema(description = "合集 ID")
    private Long collectionId;

    @Schema(description = "标签 ID 列表")
    private List<Long> tagIds;

    @Schema(description = "状态：0=草稿 1=已发布 2=已下架", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文章状态不能为空")
    private Integer status;

    @Schema(description = "是否置顶：0=否 1=是")
    private Integer isStick;

    @Schema(description = "是否加入轮播：0=否 1=是")
    private Integer isCarousel;

    @Schema(description = "轮播排序")
    private Integer carouselSort;

    @Schema(description = "是否原创：0=转载 1=原创", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原创状态不能为空")
    private Integer isOriginal;

    @Schema(description = "转载原文链接")
    @Size(max = 500, message = "转载原文链接最多500个字符")
    private String originalUrl;
}
