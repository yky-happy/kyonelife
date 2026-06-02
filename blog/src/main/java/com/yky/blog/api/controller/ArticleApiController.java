package com.yky.blog.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.api.dto.ArchiveMonthVO;
import com.yky.blog.api.dto.ArticleCardVO;
import com.yky.blog.api.dto.ArticleWebDetailVO;
import com.yky.blog.api.service.ArticleApiService;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台文章")
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleApiController {

    private final ArticleApiService articleApiService;

    @Operation(summary = "前台文章分页")
    @GetMapping("/page")
    public Result<IPage<ArticleCardVO>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long tagId,
                                             @RequestParam(required = false) Long collectionId) {
        return Result.success(articleApiService.pageArticle(page, size, keyword, tagId, collectionId));
    }

    @Operation(summary = "前台文章详情")
    @GetMapping("/{id}")
    public Result<ArticleWebDetailVO> detail(@PathVariable Long id) {
        return Result.success(articleApiService.getArticleDetail(id));
    }

    @Operation(summary = "文章归档")
    @GetMapping("/archive")
    public Result<List<ArchiveMonthVO>> archive() {
        return Result.success(articleApiService.listArchive());
    }
}
