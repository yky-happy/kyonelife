package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI 内容辅助入参（摘要生成 / 标签推荐共用）")
public class AiAssistDTO {

    @Schema(description = "文章标题")
    @Size(max = 200, message = "标题最多200个字符")
    private String title;

    @Schema(description = "Markdown 正文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "正文不能为空")
    private String contentMd;
}
