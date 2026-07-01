package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 选题助手入参。
 */
@Data
@Schema(description = "AI 选题助手入参")
public class AgentTopicsDTO {

    @Schema(description = "统计天数，默认 7")
    @Min(value = 1, message = "天数至少为 1")
    @Max(value = 90, message = "天数最多 90")
    private Integer days = 7;

    @Schema(description = "希望产出的选题数量，默认 5")
    @Min(value = 1, message = "选题数至少为 1")
    @Max(value = 10, message = "选题数最多 10")
    private Integer count = 5;

    @Schema(description = "偏好方向，如 \"后端/Redis 方向\"，可空")
    @Size(max = 50, message = "偏好方向最多 50 字")
    private String direction;

    @Schema(description = "会话 ID；同一 ID 下保留最近对话上下文，可空")
    @Size(max = 64, message = "会话 ID 最多 64 字")
    private String sessionId;
}
