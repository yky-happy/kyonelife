package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 选题助手返回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 选题助手返回")
public class AgentTopicsVO {

    @Schema(description = "选题列表")
    private List<TopicSuggestionVO> topics;

    @Schema(description = "本次迭代轮数（模型调用次数）")
    private Integer rounds;

    @Schema(description = "是否因达到迭代上限被强制收口")
    private Boolean capped;

    @Schema(description = "是否降级（AI/工具不可用时为 true）")
    private Boolean degraded;
}
