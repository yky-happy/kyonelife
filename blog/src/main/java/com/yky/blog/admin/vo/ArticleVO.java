package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "文章列表项")
public class ArticleVO {
    private Long id;
    private String title;
    private String cover;
    private String summary;
    private Long collectionId;
    private String collectionName;
    private List<TagVO> tags;
    private Integer status;
    private Integer isStick;
    private Integer isCarousel;
    private Long viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
