package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 单条 AI 选题建议（强类型结构化输出）。
 */
@Data
@Schema(description = "AI 选题建议")
public class TopicSuggestionVO {

    @Schema(description = "选题标题")
    private String title;

    @Schema(description = "推荐理由（引用了哪些热搜词/热门文章）")
    private String reason;

    @Schema(description = "引用到的热搜词")
    private List<String> refKeywords;

    @Schema(description = "参考的已有文章标题")
    private List<String> refArticles;
}
