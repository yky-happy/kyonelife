package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "合集详情（含文章数）")
public class CollectionVO {
    private Long id;
    private String name;
    private String cover;
    private String description;
    private Integer sort;
    private LocalDateTime createTime;
    private Long articleCount;
}
