package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.yky.blog.admin.service.AnalyticsService;
import com.yky.blog.admin.vo.AnalyticsOverviewVO;
import com.yky.blog.admin.vo.AnalyticsRankVO;
import com.yky.blog.admin.vo.AnalyticsTrendVO;
import com.yky.blog.admin.vo.ArticleTrendVO;
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
    @SaCheckPermission("analytics:list")
    @GetMapping("/overview")
    public Result<AnalyticsOverviewVO> overview() {
        return Result.success(analyticsService.overview());
    }

    @Operation(summary = "PV/UV 趋势")
    @SaCheckPermission("analytics:list")
    @GetMapping("/trend")
    public Result<List<AnalyticsTrendVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(analyticsService.trend(days));
    }

    @Operation(summary = "热门文章")
    @SaCheckPermission("analytics:list")
    @GetMapping("/hot-articles")
    public Result<List<HotArticleVO>> hotArticles(@RequestParam(defaultValue = "7") int days,
                                                  @RequestParam(defaultValue = "10") int limit) {
        return Result.success(analyticsService.hotArticles(days, limit));
    }

    @Operation(summary = "文章阅读趋势")
    @SaCheckPermission("analytics:list")
    @GetMapping("/article-trend")
    public Result<List<ArticleTrendVO>> articleTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(analyticsService.articleTrend(days));
    }

    @Operation(summary = "热门标签")
    @SaCheckPermission("analytics:list")
    @GetMapping("/hot-tags")
    public Result<List<AnalyticsRankVO>> hotTags(@RequestParam(defaultValue = "7") int days,
                                                 @RequestParam(defaultValue = "10") int limit) {
        return Result.success(analyticsService.hotTags(days, limit));
    }

    @Operation(summary = "热门合集")
    @SaCheckPermission("analytics:list")
    @GetMapping("/hot-collections")
    public Result<List<AnalyticsRankVO>> hotCollections(@RequestParam(defaultValue = "7") int days,
                                                        @RequestParam(defaultValue = "10") int limit) {
        return Result.success(analyticsService.hotCollections(days, limit));
    }

    @Operation(summary = "热门搜索关键词")
    @SaCheckPermission("analytics:list")
    @GetMapping("/hot-keywords")
    public Result<List<AnalyticsRankVO>> hotKeywords(@RequestParam(defaultValue = "7") int days,
                                                     @RequestParam(defaultValue = "10") int limit) {
        return Result.success(analyticsService.hotKeywords(days, limit));
    }
}
