package com.yky.blog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "前台合集")
public class CollectionApiVO {
    private Long id;
    private String name;
    private String cover;
    private String description;
    private Integer sort;
    private Long articleCount;
    private LocalDateTime createTime;
}
