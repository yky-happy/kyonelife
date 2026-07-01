package com.yky.blog.api.controller;

import com.yky.blog.admin.service.WebConfigService;
import com.yky.blog.common.entity.WebConfig;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前台站点配置")
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigApiController {

    private final WebConfigService webConfigService;

    @Operation(summary = "获取站点配置（站点名/作者/社交/公告/页脚等）")
    @GetMapping
    public Result<WebConfig> config() {
        return Result.success(webConfigService.getCachedConfig());
    }
}
