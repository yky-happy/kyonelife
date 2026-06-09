package com.yky.blog.admin.vo;

import lombok.Data;

@Data
public class HotArticleVO {

    private Long articleId;

    private String title;

    private Long viewCount;
}
