package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 摘要生成结果")
public class AiSummaryVO {

    @Schema(description = "生成的摘要")
    private String summary;

    @Schema(description = "是否为降级结果（AI 不可用时由本地规则生成）")
    private boolean degraded;

    @Schema(description = "是否命中缓存（复用历史结果，未消耗 token）")
    private boolean fromCache;
}
