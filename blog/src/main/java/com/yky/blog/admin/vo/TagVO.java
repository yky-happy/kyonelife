package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "标签详情（含文章数）")
public class TagVO {
    private Long id;
    private String name;
    private String color;
    private LocalDateTime createTime;
    private Long articleCount;
}
