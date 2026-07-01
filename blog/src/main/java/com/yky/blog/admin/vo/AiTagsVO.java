package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 标签推荐结果")
public class AiTagsVO {

    @Schema(description = "推荐的标签名列表")
    private List<String> tags;

    @Schema(description = "是否为降级结果（AI 不可用时由关键词匹配生成）")
    private boolean degraded;

    @Schema(description = "是否命中缓存")
    private boolean fromCache;
}
