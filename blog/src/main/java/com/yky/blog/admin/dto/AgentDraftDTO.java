package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 内容创作助手入参。
 */
@Data
@Schema(description = "AI 内容创作（生成草稿）入参")
public class AgentDraftDTO {

    @Schema(description = "选题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "选题不能为空")
    @Size(max = 200, message = "选题最多 200 字")
    private String topic;

    @Schema(description = "要点/提纲，可空")
    @Size(max = 1000, message = "要点最多 1000 字")
    private String points;

    @Schema(description = "写作风格，可空，如 \"务实、技术向\"")
    @Size(max = 50, message = "风格描述最多 50 字")
    private String style;

    @Schema(description = "会话 ID；同一 ID 下保留最近对话上下文，可空")
    @Size(max = 64, message = "会话 ID 最多 64 字")
    private String sessionId;
}
