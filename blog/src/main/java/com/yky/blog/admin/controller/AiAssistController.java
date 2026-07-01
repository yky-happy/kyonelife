package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.yky.blog.admin.dto.AiAssistDTO;
import com.yky.blog.admin.service.AiAssistService;
import com.yky.blog.admin.vo.AiSummaryVO;
import com.yky.blog.admin.vo.AiTagsVO;
import com.yky.blog.common.annotation.RateLimit;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 内容辅助")
@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AiAssistController {

    private final AiAssistService aiAssistService;

    @Operation(summary = "AI 生成文章摘要")
    @SaCheckLogin
    @RateLimit(name = "ai-summary", window = 60, limit = 20, message = "AI 摘要生成过于频繁，请稍后再试")
    @PostMapping("/summary")
    public Result<AiSummaryVO> summary(@Valid @RequestBody AiAssistDTO dto) {
        return Result.success(aiAssistService.generateSummary(dto));
    }

    @Operation(summary = "AI 推荐标签")
    @SaCheckLogin
    @RateLimit(name = "ai-tags", window = 60, limit = 20, message = "AI 标签推荐过于频繁，请稍后再试")
    @PostMapping("/tags")
    public Result<AiTagsVO> tags(@Valid @RequestBody AiAssistDTO dto) {
        return Result.success(aiAssistService.recommendTags(dto));
    }
}
