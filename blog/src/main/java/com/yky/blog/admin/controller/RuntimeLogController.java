package com.yky.blog.admin.controller;

import com.yky.blog.admin.service.RuntimeLogService;
import com.yky.blog.admin.vo.RuntimeLogApiMetricVO;
import com.yky.blog.admin.vo.RuntimeLogLineVO;
import com.yky.blog.admin.vo.RuntimeLogSlowRequestVO;
import com.yky.blog.admin.vo.RuntimeLogSummaryVO;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "运行日志分析")
@RestController
@RequestMapping("/admin/log")
@RequiredArgsConstructor
public class RuntimeLogController {

    private final RuntimeLogService runtimeLogService;

    @Operation(summary = "最近运行日志")
    @GetMapping("/recent")
    public Result<List<RuntimeLogLineVO>> recent(@RequestParam(defaultValue = "200") int lines,
                                                 @RequestParam(required = false) String level) {
        return Result.success(runtimeLogService.recent(lines, level));
    }

    @Operation(summary = "搜索运行日志")
    @GetMapping("/search")
    public Result<List<RuntimeLogLineVO>> search(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String level,
                                                 @RequestParam(defaultValue = "200") int lines) {
        return Result.success(runtimeLogService.search(keyword, level, lines));
    }

    @Operation(summary = "运行状态概览")
    @GetMapping("/summary")
    public Result<RuntimeLogSummaryVO> summary(@RequestParam(defaultValue = "100") long slowThreshold) {
        return Result.success(runtimeLogService.summary(slowThreshold));
    }

    @Operation(summary = "慢请求列表")
    @GetMapping("/slow-requests")
    public Result<List<RuntimeLogSlowRequestVO>> slowRequests(@RequestParam(defaultValue = "100") long threshold,
                                                              @RequestParam(defaultValue = "10") int limit) {
        return Result.success(runtimeLogService.slowRequests(threshold, limit));
    }

    @Operation(summary = "接口访问排行")
    @GetMapping("/top-apis")
    public Result<List<RuntimeLogApiMetricVO>> topApis(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(runtimeLogService.topApis(limit));
    }
}
