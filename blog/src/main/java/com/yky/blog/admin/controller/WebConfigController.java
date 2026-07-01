package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.yky.blog.admin.dto.WebConfigDTO;
import com.yky.blog.admin.service.WebConfigService;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.entity.WebConfig;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "网站配置")
@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class WebConfigController {

    private final WebConfigService webConfigService;

    @Operation(summary = "获取网站配置")
    @SaCheckPermission("config:list")
    @GetMapping
    public Result<WebConfig> get() {
        return Result.success(webConfigService.getConfig());
    }

    @Operation(summary = "更新网站配置")
    @SaCheckPermission("config:edit")
    @OperationLogRecord(module = "网站配置", operation = "更新网站配置")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody WebConfigDTO dto) {
        webConfigService.updateConfig(dto);
        return Result.success();
    }
}
