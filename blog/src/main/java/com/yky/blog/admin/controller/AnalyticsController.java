package com.yky.blog.admin.controller;

import com.yky.blog.admin.service.AnalyticsService;
import com.yky.blog.admin.vo.AnalyticsOverviewVO;
import com.yky.blog.admin.vo.AnalyticsTrendVO;
import com.yky.blog.admin.vo.HotArticleVO;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "数据分析看板")
@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "访问数据概览")
    @GetMapping("/overview")
    public Result<AnalyticsOverviewVO> overview() {
        return Result.success(analyticsService.overview());
    }

    @Operation(summary = "PV/UV 趋势")
    @GetMapping("/trend")
    public Result<List<AnalyticsTrendVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(analyticsService.trend(days));
    }

    @Operation(summary = "热门文章")
    @GetMapping("/hot-articles")
    public Result<List<HotArticleVO>> hotArticles(@RequestParam(defaultValue = "7") int days,
                                                  @RequestParam(defaultValue = "10") int limit) {
        return Result.success(analyticsService.hotArticles(days, limit));
    }
}
