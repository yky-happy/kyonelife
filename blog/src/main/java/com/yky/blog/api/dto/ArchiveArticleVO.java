package com.yky.blog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "归档文章")
public class ArchiveArticleVO {
    private Long id;
    private String title;
    private String summary;
    private LocalDateTime createTime;
}
