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
}
