package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.yky.blog.admin.dto.AgentDraftDTO;
import com.yky.blog.admin.dto.AgentTopicsDTO;
import com.yky.blog.admin.service.ContentAgentService;
import com.yky.blog.admin.vo.AgentDraftVO;
import com.yky.blog.admin.vo.AgentTopicsVO;
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

/**
 * 第二层：工具型 Agent 接口（创作助手）。
 * 限流比第一层更严——一次 Agent 会多轮调用模型，更贵。
 */
@Tag(name = "AI 创作助手（工具型 Agent）")
@RestController
@RequestMapping("/admin/ai/agent")
@RequiredArgsConstructor
public class ContentAgentController {

    private final ContentAgentService contentAgentService;

    @Operation(summary = "选题助手：基于埋点数据生成选题列表")
    @SaCheckLogin
    @SaCheckPermission("ai-agent:topics")
    @RateLimit(name = "ai-agent-topics", window = 60, limit = 10, message = "选题助手调用过于频繁，请稍后再试")
    @PostMapping("/topics")
    public Result<AgentTopicsVO> topics(@Valid @RequestBody AgentTopicsDTO dto) {
        return Result.success(contentAgentService.suggestTopics(dto, StpUtil.getLoginId()));
    }

    @Operation(summary = "内容创作：按选题/要点生成文章草稿并入库（status=0）")
    @SaCheckLogin
    @SaCheckPermission("ai-agent:draft")
    @RateLimit(name = "ai-agent-draft", window = 60, limit = 10, message = "创作助手调用过于频繁，请稍后再试")
    @PostMapping("/draft")
    public Result<AgentDraftVO> draft(@Valid @RequestBody AgentDraftDTO dto) {
        return Result.success(contentAgentService.generateDraft(dto, StpUtil.getLoginId()));
    }
}
