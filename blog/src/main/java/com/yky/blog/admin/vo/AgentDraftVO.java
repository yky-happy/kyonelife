package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 内容创作助手返回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 内容创作（生成草稿）返回")
public class AgentDraftVO {

    @Schema(description = "草稿ID（已落库 status=0，可在后台草稿列表看到）")
    private Long draftId;

    @Schema(description = "草稿标题")
    private String title;

    @Schema(description = "草稿摘要")
    private String summary;

    @Schema(description = "草稿标签")
    private List<String> tags;

    @Schema(description = "本次迭代轮数（模型调用次数）")
    private Integer rounds;

    @Schema(description = "是否因达到迭代上限被强制收口")
    private Boolean capped;

    @Schema(description = "是否降级")
    private Boolean degraded;
}
