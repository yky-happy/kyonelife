package com.yky.blog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "按月份分组的文章归档")
public class ArchiveMonthVO {
    private String month;
    private List<ArchiveArticleVO> articles;
}
