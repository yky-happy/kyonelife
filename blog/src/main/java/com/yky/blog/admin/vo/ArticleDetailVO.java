package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文章详情")
public class ArticleDetailVO extends ArticleVO {
    private String content;
    private String contentMd;
    private String keywords;
    private String aiDescribe;
    private String originalUrl;
    private Integer carouselSort;
    private Integer isOriginal;
    private List<Long> tagIds;
}
