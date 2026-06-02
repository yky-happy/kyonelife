package com.yky.blog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "前台文章列表项")
public class ArticleCardVO {
    private Long id;
    private String title;
    private String cover;
    private String summary;
    private Long collectionId;
    private String collectionName;
    private List<TagApiVO> tags;
    private Integer isStick;
    private Long viewCount;
    private LocalDateTime createTime;
}
