package com.yky.blog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "前台文章详情")
public class ArticleWebDetailVO extends ArticleCardVO {
    private String content;
    private String contentMd;
    private String keywords;
    private Integer isOriginal;
    private String originalUrl;

    @Schema(description = "同合集上一篇（更早），无则为 null")
    private ArticleNavVO prevArticle;

    @Schema(description = "同合集下一篇（更晚），无则为 null")
    private ArticleNavVO nextArticle;

    @Schema(description = "文章图集（多图 URL）")
    private java.util.List<String> images;
}
