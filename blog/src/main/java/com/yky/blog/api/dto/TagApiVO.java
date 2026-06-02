package com.yky.blog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "前台标签")
public class TagApiVO {
    private Long id;
    private String name;
    private String color;
    private Long articleCount;
}
