package com.yky.blog.admin.vo;

import lombok.Data;

@Data
public class ArticleTrendVO {

    private String date;

    private Long viewCount;

    private Long visitorCount;
}
